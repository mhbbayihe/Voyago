package cm.enspd.voyago.booking.application;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.booking.domain.BookedTrip;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.booking.domain.BookingStatus;
import cm.enspd.voyago.booking.domain.RefundPolicy;
import cm.enspd.voyago.booking.domain.RefundPolicy.RefundQuote;
import cm.enspd.voyago.booking.domain.SeatReservation;
import cm.enspd.voyago.booking.domain.SeatUnavailableException;
import cm.enspd.voyago.booking.domain.Ticket;
import cm.enspd.voyago.booking.domain.TicketStatus;
import cm.enspd.voyago.booking.infrastructure.BookingRepository;
import cm.enspd.voyago.booking.infrastructure.SeatReservationRepository;
import cm.enspd.voyago.booking.infrastructure.TicketRepository;
import cm.enspd.voyago.shared.audit.AuditService;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.ForbiddenException;
import cm.enspd.voyago.shared.domain.NotFoundException;
import cm.enspd.voyago.shared.events.DomainEvents.BookingCancelled;
import cm.enspd.voyago.shared.events.DomainEvents.BookingConfirmed;
import cm.enspd.voyago.shared.events.DomainEvents.BookingExpired;
import cm.enspd.voyago.shared.events.DomainEvents.DepartureReminder;
import cm.enspd.voyago.shared.events.DomainEvents.LatePaymentRefundRequested;
import cm.enspd.voyago.shared.events.DomainEvents.PaymentConfirmed;
import cm.enspd.voyago.trip.application.TripService;
import cm.enspd.voyago.trip.application.TripView;

/** Cas d'utilisation du module réservation : réserver, confirmer, annuler, expirer. */
@Service
public class BookingService {

	private final BookingRepository bookings;
	private final SeatReservationRepository seats;
	private final TicketRepository tickets;
	private final TripService trips;
	private final QrCodeSigner signer;
	private final RefundPolicy refundPolicy;
	private final AuditService audit;
	private final ApplicationEventPublisher events;
	private final TransactionTemplate tx;
	private final int lockMinutes;
	private final int maxPassengers;

	public BookingService(BookingRepository bookings, SeatReservationRepository seats, TicketRepository tickets,
			TripService trips, QrCodeSigner signer, RefundPolicy refundPolicy, AuditService audit,
			ApplicationEventPublisher events, TransactionTemplate tx,
			@Value("${voyago.booking.seat-lock-minutes}") int lockMinutes,
			@Value("${voyago.booking.max-passengers}") int maxPassengers) {
		this.bookings = bookings;
		this.seats = seats;
		this.tickets = tickets;
		this.trips = trips;
		this.signer = signer;
		this.refundPolicy = refundPolicy;
		this.audit = audit;
		this.events = events;
		this.tx = tx;
		this.lockMinutes = lockMinutes;
		this.maxPassengers = maxPassengers;
	}

	public record PassengerInput(String firstName, String lastName, String idNumber, Integer seatNumber) {
	}

	public record Customer(UUID id, String name, String email, String phone) {
		public static Customer of(AppUserPrincipal p) {
			return new Customer(p.id(), p.fullName(), p.email(), p.phone());
		}
	}

	// ------------------------------------------------------------------ Création (F08, F09)

	/**
	 * Crée une réservation « en attente de paiement » et verrouille les sièges pour 15 minutes.
	 * Si deux clients prennent le même siège au même moment, la contrainte d'unicité de la base
	 * rejette le second (RG1) : on le traduit en message clair.
	 */
	public Booking create(Customer customer, UUID tripId, List<PassengerInput> passengers, UUID soldByAgentId) {
		try {
			return tx.execute(status -> doCreate(customer, tripId, passengers, soldByAgentId));
		} catch (DataIntegrityViolationException e) {
			throw new SeatUnavailableException(List.of());
		}
	}

	private Booking doCreate(Customer customer, UUID tripId, List<PassengerInput> passengers, UUID soldByAgentId) {
		if (passengers == null || passengers.isEmpty() || passengers.size() > maxPassengers) {
			throw new BusinessException("PASSENGER_COUNT", "Une réservation comporte de 1 à " + maxPassengers + " passagers");
		}
		TripView trip = trips.requireBookable(tripId);
		Set<Integer> requested = new LinkedHashSet<>();
		for (PassengerInput p : passengers) {
			if (p.seatNumber() == null || p.seatNumber() < 1 || p.seatNumber() > trip.seatCount()) {
				throw new BusinessException("INVALID_SEAT", "Numéro de siège invalide");
			}
			if (!requested.add(p.seatNumber())) {
				throw new BusinessException("DUPLICATE_SEAT", "Chaque passager doit avoir un siège différent");
			}
			if (isBlank(p.firstName()) || isBlank(p.lastName()) || isBlank(p.idNumber())) {
				throw new BusinessException("PASSENGER_INFO",
						"Nom, prénom et numéro de pièce d'identité sont obligatoires pour chaque passager");
			}
		}

		LocalDateTime now = LocalDateTime.now();
		releaseExpiredLocks(tripId, now);
		List<Integer> taken = seats.findByTripIdAndSeatNumberIn(tripId, requested).stream()
				.map(SeatReservation::getSeatNumber).sorted().toList();
		if (!taken.isEmpty()) {
			throw new SeatUnavailableException(taken);
		}

		BookedTrip snapshot = new BookedTrip(trip.id(), trip.companyId(), trip.companyName(), trip.mode(), trip.label(),
				trip.fromStation(), trip.toStation(), trip.serviceClassLabel(), trip.departureAt(), trip.price(),
				trip.seatCount());
		Booking booking = Booking.create(customer.id(), customer.name(), customer.email(), customer.phone(), snapshot,
				now, lockMinutes);
		passengers.forEach(p -> booking.addPassenger(p.firstName(), p.lastName(), p.idNumber(), p.seatNumber()));
		if (soldByAgentId != null) {
			booking.markSoldBy(soldByAgentId);
		}
		bookings.save(booking);
		seats.saveAllAndFlush(requested.stream().map(s -> new SeatReservation(tripId, s, booking.getId())).toList());
		return booking;
	}

	// ------------------------------------------------------------------ Saga : réaction aux paiements

	/** Paiement confirmé → réservation confirmée et billets émis (F14). */
	@EventListener
	@Transactional
	public void onPaymentConfirmed(PaymentConfirmed event) {
		Booking booking = bookings.findById(event.bookingId()).orElseThrow(() -> new NotFoundException("Réservation"));
		if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
			// Paiement arrivé trop tard (réservation expirée ou annulée) : action de compensation.
			events.publishEvent(new LatePaymentRefundRequested(booking.getId(), event.paymentId(), event.amount()));
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		booking.confirm(now);
		for (var passenger : booking.getPassengers()) {
			String code = newTicketCode();
			tickets.save(new Ticket(code, signer.sign(code), booking, passenger, now));
		}
		events.publishEvent(new BookingConfirmed(booking.getId(), booking.getReference(), booking.getCustomerId(),
				booking.getCustomerEmail(), booking.getCustomerName(), booking.getCustomerPhone(), booking.getTripLabel(),
				booking.getDepartureAt(), booking.getTotalAmount()));
	}

	private String newTicketCode() {
		String code;
		do {
			code = "TK-" + Booking.randomCode(8);
		} while (tickets.existsByCode(code));
		return code;
	}

	// ------------------------------------------------------------------ Annulation (F10, RG5)

	@Transactional
	public Booking cancel(AppUserPrincipal actor, UUID bookingId) {
		Booking booking = get(bookingId);
		if (!booking.isOwnedBy(actor.id()) && !actor.isAdmin()) {
			throw new ForbiddenException("Vous ne pouvez annuler que vos propres réservations");
		}
		LocalDateTime now = LocalDateTime.now();
		if (booking.getDepartureAt().isBefore(now)) {
			throw new BusinessException("TRIP_DEPARTED", "Le voyage est déjà parti : annulation impossible");
		}
		List<Ticket> bookingTickets = tickets.findByBookingIdOrderBySeatNumber(bookingId);
		if (bookingTickets.stream().anyMatch(t -> t.getStatus() == TicketStatus.USED)) {
			throw new BusinessException("TICKET_USED", "Un billet de cette réservation a déjà été utilisé");
		}
		boolean paid = booking.getStatus() == BookingStatus.CONFIRMED;
		long refund = paid ? refundPolicy.quote(booking.getTotalAmount(), booking.getDepartureAt(), now).amount() : 0;
		booking.cancel(refund, now);
		bookingTickets.forEach(Ticket::cancel);
		seats.deleteByBookingId(bookingId);
		audit.record("ANNULATION_RESERVATION", booking.getReference() + " - remboursement " + refund + " XAF");
		events.publishEvent(new BookingCancelled(booking.getId(), booking.getReference(), booking.getCustomerId(),
				booking.getCustomerEmail(), booking.getCustomerName(), booking.getTripLabel(), paid, refund));
		return booking;
	}

	public RefundQuote refundQuote(Booking booking) {
		if (booking.getStatus() != BookingStatus.CONFIRMED) {
			return new RefundQuote(0, 0);
		}
		return refundPolicy.quote(booking.getTotalAmount(), booking.getDepartureAt(), LocalDateTime.now());
	}

	public String refundPolicyDescription() {
		return refundPolicy.describe();
	}

	// ------------------------------------------------------------------ Expiration (RG2) et rappels

	@Transactional
	public int expireOverdue() {
		List<Booking> overdue = bookings.findByStatusAndExpiresAtBefore(BookingStatus.PENDING_PAYMENT, LocalDateTime.now());
		overdue.forEach(this::expire);
		return overdue.size();
	}

	private void releaseExpiredLocks(UUID tripId, LocalDateTime now) {
		bookings.findByTripIdAndStatusAndExpiresAtBefore(tripId, BookingStatus.PENDING_PAYMENT, now).forEach(this::expire);
	}

	private void expire(Booking booking) {
		booking.expire();
		seats.deleteByBookingId(booking.getId());
		events.publishEvent(new BookingExpired(booking.getId(), booking.getReference(), booking.getCustomerId(),
				booking.getCustomerEmail(), booking.getCustomerName(), booking.getTripLabel()));
	}

	@Transactional
	public void sendDepartureReminders() {
		LocalDateTime now = LocalDateTime.now();
		for (Booking b : bookings.findByStatusAndReminderSentFalseAndDepartureAtBetween(BookingStatus.CONFIRMED, now,
				now.plusHours(24))) {
			b.markReminderSent();
			events.publishEvent(new DepartureReminder(b.getId(), b.getReference(), b.getCustomerId(), b.getCustomerEmail(),
					b.getCustomerName(), b.getCustomerPhone(), b.getTripLabel(), b.getDepartureAt()));
		}
	}

	// ------------------------------------------------------------------ Consultation

	@Transactional(readOnly = true)
	public Booking get(UUID id) {
		return bookings.findById(id).orElseThrow(() -> new NotFoundException("Réservation"));
	}

	/** Client propriétaire, agent de la compagnie (RG7) ou administrateur. */
	@Transactional(readOnly = true)
	public Booking getAccessible(AppUserPrincipal user, UUID id) {
		Booking booking = get(id);
		boolean allowed = booking.isOwnedBy(user.id()) || user.isAdmin()
				|| (user.isAgent() && booking.getCompanyId().equals(user.companyId()));
		if (!allowed) {
			throw new ForbiddenException("Cette réservation ne vous appartient pas");
		}
		return booking;
	}

	/** Utilisé par le module payment : la réservation doit être en attente et dans le délai de 15 min. */
	@Transactional(readOnly = true)
	public Booking requirePayable(AppUserPrincipal user, UUID bookingId) {
		Booking booking = getAccessible(user, bookingId);
		LocalDateTime now = LocalDateTime.now();
		if (booking.isLockExpired(now)) {
			throw new BusinessException("LOCK_EXPIRED",
					"Le délai de " + lockMinutes + " minutes est dépassé : les sièges ont été libérés. Merci de refaire une réservation.");
		}
		if (!booking.isPayable(now)) {
			throw new BusinessException("NOT_PAYABLE", "Cette réservation est « " + booking.getStatus().getLabel() + " »");
		}
		return booking;
	}

	@Transactional(readOnly = true)
	public List<Booking> forCustomer(UUID customerId) {
		return bookings.findByCustomerIdOrderByDepartureAtDesc(customerId);
	}

	@Transactional(readOnly = true)
	public List<Booking> forCompany(UUID companyId) {
		return bookings.findByCompanyIdOrderByCreatedAtDesc(companyId);
	}

	@Transactional(readOnly = true)
	public List<Booking> all() {
		return bookings.findAllByOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
	public List<Ticket> tickets(UUID bookingId) {
		return tickets.findByBookingIdOrderBySeatNumber(bookingId);
	}

	/** Liste des passagers d'un voyage (manifeste) pour l'agent de la compagnie. */
	@Transactional(readOnly = true)
	public List<Ticket> manifest(AppUserPrincipal agent, UUID tripId) {
		TripView trip = trips.view(tripId);
		if (agent.isAgent() && !trip.companyId().equals(agent.companyId())) {
			throw new ForbiddenException("Ce voyage n'appartient pas à votre compagnie");
		}
		return tickets.findByTripIdOrderBySeatNumber(tripId);
	}

	public int getLockMinutes() {
		return lockMinutes;
	}

	public int getMaxPassengers() {
		return maxPassengers;
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}
}
