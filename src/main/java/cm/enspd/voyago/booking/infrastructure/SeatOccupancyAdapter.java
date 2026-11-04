package cm.enspd.voyago.booking.infrastructure;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import cm.enspd.voyago.trip.domain.port.SeatOccupancyPort;

/** Adaptateur : implémente le port du module trip à partir des occupations de sièges du module booking. */
@Component
class SeatOccupancyAdapter implements SeatOccupancyPort {

	private final SeatReservationRepository reservations;

	SeatOccupancyAdapter(SeatReservationRepository reservations) {
		this.reservations = reservations;
	}

	@Override
	public Set<Integer> occupiedSeats(UUID tripId) {
		return new HashSet<>(reservations.findSeatNumbers(tripId));
	}

	@Override
	public Map<UUID, Long> occupiedCount(Collection<UUID> tripIds) {
		Map<UUID, Long> result = new HashMap<>();
		if (tripIds.isEmpty()) {
			return result;
		}
		for (Object[] row : reservations.countByTrip(tripIds)) {
			result.put((UUID) row[0], (Long) row[1]);
		}
		return result;
	}
}
