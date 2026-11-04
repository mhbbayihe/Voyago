package cm.enspd.voyago.payment.web;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.payment.application.PaymentService;
import cm.enspd.voyago.payment.domain.Payment;
import cm.enspd.voyago.payment.domain.PaymentMethod;
import cm.enspd.voyago.payment.domain.PaymentStatus;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.ForbiddenException;

@Controller
public class PaymentWebController {

	private final BookingService bookings;
	private final PaymentService payments;

	public PaymentWebController(BookingService bookings, PaymentService payments) {
		this.bookings = bookings;
		this.payments = payments;
	}

	@GetMapping("/bookings/{bookingId}/pay")
	public String payPage(@PathVariable UUID bookingId, @AuthenticationPrincipal AppUserPrincipal me, Model model) {
		Booking booking = bookings.getAccessible(me, bookingId);
		if (!booking.isPayable(LocalDateTime.now())) {
			return "redirect:/bookings/" + bookingId;
		}
		model.addAttribute("booking", booking);
		model.addAttribute("secondsLeft", Math.max(0, Duration.between(LocalDateTime.now(), booking.getExpiresAt()).getSeconds()));
		model.addAttribute("idempotencyKey", UUID.randomUUID().toString());
		model.addAttribute("methods", new PaymentMethod[] { PaymentMethod.MTN_MOMO, PaymentMethod.ORANGE_MONEY });
		return "payment/pay";
	}

	@PostMapping("/bookings/{bookingId}/pay")
	public String pay(@PathVariable UUID bookingId, @AuthenticationPrincipal AppUserPrincipal me,
			@RequestParam PaymentMethod method, @RequestParam String phone, @RequestParam String idempotencyKey,
			RedirectAttributes flash) {
		try {
			Payment payment = payments.initiate(me, bookingId, method, phone, idempotencyKey);
			if (payment.getStatus() == PaymentStatus.FAILED) {
				flash.addFlashAttribute("error", payment.getFailureReason());
				return "redirect:/bookings/" + bookingId + "/pay";
			}
			return "redirect:/payments/" + payment.getId();
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
			return "redirect:/bookings/" + bookingId;
		}
	}

	@GetMapping("/payments/{paymentId}")
	public String status(@PathVariable UUID paymentId, @AuthenticationPrincipal AppUserPrincipal me, Model model,
			RedirectAttributes flash) {
		Payment payment = payments.getOwned(me, paymentId);
		switch (payment.getStatus()) {
			case SUCCEEDED -> {
				flash.addFlashAttribute("success", "Paiement confirmé ! Vos billets sont prêts et ont été envoyés par e-mail.");
				return "redirect:/bookings/" + payment.getBookingId();
			}
			case FAILED, CANCELLED -> {
				flash.addFlashAttribute("error", "Paiement non abouti : " + payment.getFailureReason());
				return "redirect:/bookings/" + payment.getBookingId() + "/pay";
			}
			default -> {
			}
		}
		model.addAttribute("payment", payment);
		model.addAttribute("booking", bookings.get(payment.getBookingId()));
		return "payment/pending";
	}

	/**
	 * Simulateur de l'opérateur : remplace la validation par code PIN sur le téléphone du client.
	 * Il appelle exactement le même traitement que le webhook réel /api/v1/payments/webhook/{provider}.
	 */
	@PostMapping("/payments/{paymentId}/simulate")
	public String simulate(@PathVariable UUID paymentId, @AuthenticationPrincipal AppUserPrincipal me,
			@RequestParam String outcome) {
		Payment payment = payments.get(paymentId);
		if (!payment.getCustomerId().equals(me.id())) {
			throw new ForbiddenException("Ce paiement ne vous appartient pas");
		}
		boolean approved = "approve".equals(outcome);
		payments.handleProviderResult(payment.getMethod(), payment.getExternalReference(), approved,
				approved ? null : "Transaction refusée par le client sur son téléphone");
		return "redirect:/payments/" + paymentId;
	}
}
