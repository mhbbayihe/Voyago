package cm.enspd.voyago.booking.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import cm.enspd.voyago.trip.domain.TransportMode;

/** Objet-valeur : instantané du voyage au moment de la réservation. */
public record BookedTrip(UUID tripId, UUID companyId, String companyName, TransportMode mode, String label,
		String fromStation, String toStation, String serviceClassLabel, LocalDateTime departureAt, long unitPrice,
		int seatCount) {
}
