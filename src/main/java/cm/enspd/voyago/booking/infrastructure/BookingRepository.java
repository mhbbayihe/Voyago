package cm.enspd.voyago.booking.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.booking.domain.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	Optional<Booking> findByReference(String reference);

	boolean existsByReference(String reference);

	List<Booking> findByCustomerIdOrderByDepartureAtDesc(UUID customerId);

	List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime now);

	List<Booking> findByTripIdAndStatusAndExpiresAtBefore(UUID tripId, BookingStatus status, LocalDateTime now);

	List<Booking> findByStatusAndReminderSentFalseAndDepartureAtBetween(BookingStatus status, LocalDateTime from,
			LocalDateTime to);

	List<Booking> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

	List<Booking> findAllByOrderByCreatedAtDesc();
}
