package cm.enspd.voyago.trip.domain.port;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Port (architecture hexagonale) : le module trip a besoin de connaître les sièges occupés
 * sans dépendre du module booking. C'est le module booking qui fournit l'implémentation
 * (inversion de dépendance).
 */
public interface SeatOccupancyPort {

	Set<Integer> occupiedSeats(UUID tripId);

	Map<UUID, Long> occupiedCount(Collection<UUID> tripIds);
}
