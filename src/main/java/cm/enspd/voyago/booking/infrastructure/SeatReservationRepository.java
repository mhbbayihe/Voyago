package cm.enspd.voyago.booking.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cm.enspd.voyago.booking.domain.SeatReservation;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, UUID> {

	@Query("select s.seatNumber from SeatReservation s where s.tripId = :tripId")
	List<Integer> findSeatNumbers(@Param("tripId") UUID tripId);

	@Query("select s.tripId, count(s) from SeatReservation s where s.tripId in :tripIds group by s.tripId")
	List<Object[]> countByTrip(@Param("tripIds") Collection<UUID> tripIds);

	List<SeatReservation> findByTripIdAndSeatNumberIn(UUID tripId, Collection<Integer> seatNumbers);

	@Modifying
	@Query("delete from SeatReservation s where s.bookingId = :bookingId")
	int deleteByBookingId(@Param("bookingId") UUID bookingId);
}
