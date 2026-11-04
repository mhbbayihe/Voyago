package cm.enspd.voyago.booking.web;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.application.BookingService.Customer;
import cm.enspd.voyago.booking.application.TicketService;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.booking.domain.BookingStatus;
import cm.enspd.voyago.booking.domain.Ticket;
import cm.enspd.voyago.booking.domain.TicketStatus;
import cm.enspd.voyago.payment.application.PaymentService;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.ForbiddenException;
import cm.enspd.voyago.trip.application.TripService;

/** Parcours client : sièges → passagers → paiement → billet, et espace « Mes réservations ». */
@Controller
public class BookingWebController {

	private final TripService trips;
	private final BookingService bookings;
	private final TicketService tickets;
	private final PaymentService payments;

	public BookingWebController(TripService trips, BookingService bookings, TicketService tickets,
			PaymentService payments) {
		this.trips = trips;
		this.bookings = bookings;
		this.tickets = tickets;
		this.payments = payments;
	}

	@GetMapping("/trips/{tripId}/seats")
	public String seats(@PathVariable UUID tripId, Model model) {
		model.addAttribute("trip", trips.view(tripId));
		model.addAttribute("seatMap", trips.seatMap(tripId));
		model.addAttribute("maxPassengers", bookings.getMaxPassengers());
		model.addAttribute("lockMinutes", bookings.getLockMinutes());
		model.addAttribute("counter", false);
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new BookingForm());
		}
		return "booking/seats";
	}

	@PostMapping("/trips/{tripId}/book")
	public String book(@PathVariable UUID tripId, @AuthenticationPrincipal AppUserPrincipal me,
			@ModelAttribute("form") BookingForm form, RedirectAttributes flash) {
		try {
			Booking booking = bookings.create(Customer.of(me), tripId, form.toInputs(), null);
			return "redirect:/bookings/" + booking.getId() + "/pay";
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
			return "redirect:/trips/" + tripId + "/seats";
		}
	}

	@GetMapping("/bookings/{id}")
	public String detail(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal me, Model model) {
		Booking booking = bookings.getAccessible(me, id);
		List<Ticket> list = bookings.tickets(id);
		model.addAttribute("booking", booking);
		model.addAttribute("tickets", list);
		model.addAttribute("payments", payments.forBooking(id));
		model.addAttribute("refund", bookings.refundQuote(booking));
		model.addAttribute("refundPolicy", bookings.refundPolicyDescription());
		model.addAttribute("payable", booking.isPayable(LocalDateTime.now()));
		model.addAttribute("secondsLeft", Math.max(0, Duration.between(LocalDateTime.now(), booking.getExpiresAt()).getSeconds()));
		model.addAttribute("cancellable", booking.isOwnedBy(me.id())
				&& (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.PENDING_PAYMENT)
				&& booking.getDepartureAt().isAfter(LocalDateTime.now())
				&& list.stream().noneMatch(t -> t.getStatus() == TicketStatus.USED));
		return "booking/detail";
	}

	@PostMapping("/bookings/{id}/cancel")
	public String cancel(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal me, RedirectAttributes flash) {
		try {
			Booking booking = bookings.cancel(me, id);
			flash.addFlashAttribute("success", booking.getRefundAmount() > 0
					? "Réservation annulée. Remboursement de " + String.format("%,d", booking.getRefundAmount()) + " FCFA effectué."
					: "Réservation annulée.");
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/bookings/" + id;
	}

	@GetMapping("/account/bookings")
	public String myBookings(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
		LocalDateTime now = LocalDateTime.now();
		List<Booking> all = bookings.forCustomer(me.id());
		model.addAttribute("upcoming", all.stream().filter(b -> b.getDepartureAt().isAfter(now)
				&& (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING_PAYMENT))
				.sorted((a, b) -> a.getDepartureAt().compareTo(b.getDepartureAt())).toList());
		model.addAttribute("history", all.stream().filter(b -> !(b.getDepartureAt().isAfter(now)
				&& (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING_PAYMENT))).toList());
		return "account/bookings";
	}

	@GetMapping("/bookings/{id}/tickets.pdf")
	public ResponseEntity<byte[]> bookingPdf(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal me) {
		Booking booking = bookings.getAccessible(me, id);
		List<Ticket> list = bookings.tickets(id);
		if (list.isEmpty()) {
			throw new BusinessException("NO_TICKET", "Les billets seront disponibles après confirmation du paiement");
		}
		return pdfResponse(tickets.pdf(list), "billets-" + booking.getReference() + ".pdf");
	}

	@GetMapping("/tickets/{ticketId}/qr.png")
	public ResponseEntity<byte[]> qr(@PathVariable UUID ticketId, @AuthenticationPrincipal AppUserPrincipal me) {
		Ticket ticket = tickets.get(ticketId);
		bookings.getAccessible(me, ticket.getBookingId());
		return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(tickets.qrPng(ticket, 280));
	}

	@GetMapping("/tickets/{ticketId}/ticket.pdf")
	public ResponseEntity<byte[]> ticketPdf(@PathVariable UUID ticketId, @AuthenticationPrincipal AppUserPrincipal me) {
		Ticket ticket = tickets.get(ticketId);
		if (me == null) {
			throw new ForbiddenException("Connexion requise");
		}
		bookings.getAccessible(me, ticket.getBookingId());
		return pdfResponse(tickets.pdf(List.of(ticket)), "billet-" + ticket.getCode() + ".pdf");
	}

	static ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"").body(pdf);
	}
}
