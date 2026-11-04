package cm.enspd.voyago.payment.application;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.payment.domain.Payment;
import cm.enspd.voyago.payment.domain.PaymentMethod;
import cm.enspd.voyago.payment.domain.PaymentStatus;
import cm.enspd.voyago.payment.domain.Refund;
import cm.enspd.voyago.payment.domain.port.PaymentProvider;
import cm.enspd.voyago.payment.domain.port.PaymentProvider.ProviderResult;
import cm.enspd.voyago.payment.infrastructure.PaymentRepository;
import cm.enspd.voyago.payment.infrastructure.RefundRepository;
import cm.enspd.voyago.shared.audit.AuditService;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.ForbiddenException;
import cm.enspd.voyago.shared.domain.NotFoundException;
import cm.enspd.voyago.shared.events.DomainEvents.BookingCancelled;
import cm.enspd.voyago.shared.events.DomainEvents.BookingExpired;
import cm.enspd.voyago.shared.events.DomainEvents.LatePaymentRefundRequested;
import cm.enspd.voyago.shared.events.DomainEvents.PaymentConfirmed;
import cm.enspd.voyago.shared.events.DomainEvents.PaymentFailed;

/** Cas d'utilisation du paiement (F12, F13) et étapes de compensation de la saga. */
@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final PaymentRepository payments;
	private final RefundRepository refunds;
	private final BookingService bookings;
	private final Map<PaymentMethod, PaymentProvider> providers = new EnumMap<>(PaymentMethod.class);
	private final AuditService audit;
	private final ApplicationEventPublisher events;

	public PaymentService(PaymentRepository payments, RefundRepository refunds, BookingService bookings,
			List<PaymentProvider> providerList, AuditService audit, ApplicationEventPublisher events) {
		this.payments = payments;
		this.refunds = refunds;
		this.bookings = bookings;
		this.audit = audit;
		this.events = events;
		providerList.forEach(p -> providers.put(p.method(), p));
	}

	private PaymentProvider provider(PaymentMethod method) {
		PaymentProvider provider = providers.get(method);
		if (provider == null) {
			throw new BusinessException("UNKNOWN_PROVIDER", "Moyen de paiement non disponible");
		}
		return provider;
	}

	/**
	 * Initie un paiement. RG4 : le montant est celui de la réservation (jamais fourni par le client),
	 * et une même clé d'idempotence renvoie toujours le même paiement (pas de double débit).
	 */
	@Transactional
	public Payment initiate(AppUserPrincipal user, UUID bookingId, PaymentMethod method, String payerPhone,
			String idempotencyKey) {
		String key = idempotencyKey == null || idempotencyKey.isBlank() ? UUID.randomUUID().toString() : idempotencyKey;
		var existing = payments.findByIdempotencyKey(key);
		if (existing.isPresent()) {
			return existing.get();
		}
		if (method == PaymentMethod.CASH && !user.isAgent() && !user.isAdmin()) {
			throw new ForbiddenException("Le paiement en espèces est réservé aux ventes au guichet");
		}
		Booking booking = bookings.requirePayable(user, bookingId);
		payments.findByBookingIdAndStatus(bookingId, PaymentStatus.PENDING)
				.forEach(p -> p.cancel("Remplacé par une nouvelle tentative"));

		Payment payment = new Payment(booking.getId(), booking.getReference(), booking.getCustomerId(),
				booking.getCustomerEmail(), booking.getTotalAmount(), method, payerPhone, key);
		ProviderResult result = provider(method).initiate(payerPhone, booking.getTotalAmount(), booking.getReference());
		if (!result.accepted()) {
			payment.fail(result.message());
			payments.save(payment);
			events.publishEvent(new PaymentFailed(payment.getId(), booking.getId(), booking.getCustomerId(),
					booking.getCustomerEmail(), result.message()));
			return payment;
		}
		payment.initiated(result.externalReference());
		payments.save(payment);
		if (result.immediate()) {
			confirm(payment);
		}
		return payment;
	}

	/**
	 * Résultat transmis par le fournisseur (webhook ou simulateur). Idempotent : un webhook rejoué
	 * pour un paiement déjà traité est ignoré.
	 */
	@Transactional
	public Payment handleProviderResult(PaymentMethod method, String externalReference, boolean success, String reason) {
		Payment payment = payments.findByExternalReference(externalReference)
				.orElseThrow(() -> new NotFoundException("Transaction " + externalReference));
		if (payment.getMethod() != method) {
			throw new BusinessException("PROVIDER_MISMATCH", "Fournisseur incohérent pour cette transaction");
		}
		if (payment.getStatus() != PaymentStatus.PENDING) {
			log.info("Webhook rejoué pour {} (déjà {}), ignoré", externalReference, payment.getStatus());
			return payment;
		}
		if (success) {
			confirm(payment);
		} else {
			String why = reason == null || reason.isBlank() ? "Paiement refusé par l'opérateur" : reason;
			payment.fail(why);
			events.publishEvent(new PaymentFailed(payment.getId(), payment.getBookingId(), payment.getCustomerId(),
					payment.getCustomerEmail(), why));
		}
		return payment;
	}

	private void confirm(Payment payment) {
		payment.succeed();
		audit.record("PAIEMENT_CONFIRME", payment.getBookingReference() + " - " + payment.getAmount() + " XAF via "
				+ payment.getMethod().getLabel());
		events.publishEvent(new PaymentConfirmed(payment.getId(), payment.getBookingId(), payment.getAmount(),
				payment.getMethod().name()));
	}

	// ------------------------------------------------------------------ Compensations de la saga

	@EventListener
	@Transactional
	public void onBookingCancelled(BookingCancelled event) {
		if (event.wasPaid() && event.refundAmount() > 0) {
			payments.findByBookingIdAndStatus(event.bookingId(), PaymentStatus.SUCCEEDED).stream().findFirst()
					.ifPresent(p -> refund(p, event.refundAmount(), "Annulation par le client (barème RG5)"));
		}
	}

	@EventListener
	@Transactional
	public void onBookingExpired(BookingExpired event) {
		payments.findByBookingIdAndStatus(event.bookingId(), PaymentStatus.PENDING)
				.forEach(p -> p.cancel("Délai de paiement dépassé"));
	}

	@EventListener
	@Transactional
	public void onLatePayment(LatePaymentRefundRequested event) {
		Payment payment = payments.findById(event.paymentId()).orElseThrow(() -> new NotFoundException("Paiement"));
		refund(payment, event.amount(), "Paiement reçu après expiration de la réservation");
	}

	private void refund(Payment payment, long amount, String reason) {
		ProviderResult result = provider(payment.getMethod()).refund(payment.getExternalReference(), amount);
		payment.refund(amount);
		refunds.save(new Refund(payment, amount, result.externalReference(), reason));
		audit.record("REMBOURSEMENT", payment.getBookingReference() + " - " + amount + " XAF - " + reason);
	}

	// ------------------------------------------------------------------ Consultation

	@Transactional(readOnly = true)
	public Payment get(UUID id) {
		return payments.findById(id).orElseThrow(() -> new NotFoundException("Paiement"));
	}

	@Transactional(readOnly = true)
	public Payment getOwned(AppUserPrincipal user, UUID id) {
		Payment payment = get(id);
		if (!payment.getCustomerId().equals(user.id()) && !user.isAdmin() && !user.isAgent()) {
			throw new ForbiddenException("Ce paiement ne vous appartient pas");
		}
		return payment;
	}

	@Transactional(readOnly = true)
	public List<Payment> forBooking(UUID bookingId) {
		return payments.findByBookingIdOrderByCreatedAtDesc(bookingId);
	}

	@Transactional(readOnly = true)
	public List<Payment> all() {
		return payments.findAllByOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
	public List<Refund> allRefunds() {
		return refunds.findAllByOrderByCreatedAtDesc();
	}
}
