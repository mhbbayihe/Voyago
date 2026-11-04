package cm.enspd.voyago.booking.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.application.BookingService.Customer;
import cm.enspd.voyago.booking.application.BookingService.PassengerInput;
import cm.enspd.voyago.booking.application.TicketService;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.booking.domain.Ticket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@Tag(name = "Réservations et billets")
public class BookingApiController {

	private final BookingService bookings;
	private final TicketService tickets;

	public BookingApiController(BookingService bookings, TicketService tickets) {
		this.bookings = bookings;
		this.tickets = tickets;
	}

	public record PassengerRequest(@NotBlank String firstName, @NotBlank String lastName, @NotBlank String idNumber,
			@NotNull Integer seatNumber) {
	}

	public record CreateBookingRequest(@NotNull UUID tripId, @NotEmpty List<@Valid PassengerRequest> passengers) {
	}

	public record PassengerResponse(String name, String idNumber, int seatNumber) {
	}

	public record TicketResponse(UUID id, String code, String passengerName, int seatNumber, String status, String qrContent) {
	}

	public record BookingResponse(UUID id, String reference, String status, String tripLabel, String company,
			LocalDateTime departureAt, long totalAmount, LocalDateTime lockExpiresAt, long refundAmount,
			List<PassengerResponse> passengers, List<TicketResponse> tickets) {
	}

	private BookingResponse toResponse(Booking b) {
		List<TicketResponse> t = bookings.tickets(b.getId()).stream()
				.map(x -> new TicketResponse(x.getId(), x.getCode(), x.getPassengerName(), x.getSeatNumber(),
						x.getStatus().name(), tickets.qrPayload(x)))
				.toList();
		return new BookingResponse(b.getId(), b.getReference(), b.getStatus().name(), b.getTripLabel(), b.getCompanyName(),
				b.getDepartureAt(), b.getTotalAmount(), b.getExpiresAt(), b.getRefundAmount(),
				b.getPassengers().stream().map(p -> new PassengerResponse(p.getFullName(), p.getIdNumber(), p.getSeatNumber()))
						.toList(),
				t);
	}

	@PostMapping("/api/v1/bookings")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Créer une réservation et verrouiller les sièges 15 min (409 si un siège est déjà pris)")
	public BookingResponse create(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody CreateBookingRequest req) {
		List<PassengerInput> passengers = req.passengers().stream()
				.map(p -> new PassengerInput(p.firstName(), p.lastName(), p.idNumber(), p.seatNumber())).toList();
		return toResponse(bookings.create(Customer.of(me), req.tripId(), passengers, null));
	}

	@GetMapping("/api/v1/bookings")
	@Operation(summary = "Mes réservations")
	public List<BookingResponse> mine(@AuthenticationPrincipal AppUserPrincipal me) {
		return bookings.forCustomer(me.id()).stream().map(this::toResponse).toList();
	}

	@GetMapping("/api/v1/bookings/{id}")
	@Operation(summary = "Consulter une réservation")
	public BookingResponse get(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID id) {
		return toResponse(bookings.getAccessible(me, id));
	}

	@PostMapping("/api/v1/bookings/{id}/cancel")
	@Operation(summary = "Annuler une réservation (remboursement selon le barème RG5)")
	public BookingResponse cancel(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID id) {
		return toResponse(bookings.cancel(me, id));
	}

	@GetMapping(value = "/api/v1/tickets/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(summary = "Télécharger le billet PDF")
	public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID id) {
		Ticket ticket = tickets.get(id);
		bookings.getAccessible(me, ticket.getBookingId());
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"billet-" + ticket.getCode() + ".pdf\"")
				.body(tickets.pdf(List.of(ticket)));
	}

	public record ValidateRequest(@NotBlank String code) {
	}

	public record ValidateResponse(boolean valid, String message, String passengerName, Integer seatNumber, String trip) {
	}

	@PostMapping("/api/v1/tickets/validate")
	@Operation(summary = "Valider un billet par QR code (agent) - un second scan est refusé")
	public ValidateResponse validate(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody ValidateRequest req) {
		var r = tickets.validate(req.code(), me);
		var t = r.ticket();
		return new ValidateResponse(r.valid(), r.message(), t == null ? null : t.getPassengerName(),
				t == null ? null : t.getSeatNumber(), t == null ? null : t.getTripLabel());
	}
}
