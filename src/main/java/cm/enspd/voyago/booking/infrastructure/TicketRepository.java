package cm.enspd.voyago.booking.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cm.enspd.voyago.booking.domain.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

	List<Ticket> findByBookingIdOrderBySeatNumber(UUID bookingId);

	List<Ticket> findByTripIdOrderBySeatNumber(UUID tripId);

	Optional<Ticket> findByCode(String code);

	boolean existsByCode(String code);
}
