package cm.enspd.voyago.booking.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Occupation d'un siège par une réservation active (en attente de paiement ou confirmée).
 * La contrainte d'unicité (trip_id, seat_number) garantit RG1 : même si deux clients valident
 * au même instant, la base refuse le second. La ligne est supprimée à l'expiration (RG2) ou à l'annulation.
 * C'est l'équivalent persistant du verrou Redis « SET siege:{voyage}:{numero} NX EX 900 » du cahier des charges.
 */
@Entity
@Table(name = "seat_reservation", uniqueConstraints = @UniqueConstraint(name = "uk_trip_seat", columnNames = {
		"trip_id", "seat_number" }))
public class SeatReservation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "trip_id", nullable = false)
	private UUID tripId;

	@Column(name = "seat_number", nullable = false)
	private int seatNumber;

	@Column(nullable = false)
	private UUID bookingId;

	protected SeatReservation() {
	}

	public SeatReservation(UUID tripId, int seatNumber, UUID bookingId) {
		this.tripId = tripId;
		this.seatNumber = seatNumber;
		this.bookingId = bookingId;
	}

	public UUID getId() { return id; }
	public UUID getTripId() { return tripId; }
	public int getSeatNumber() { return seatNumber; }
	public UUID getBookingId() { return bookingId; }
}
