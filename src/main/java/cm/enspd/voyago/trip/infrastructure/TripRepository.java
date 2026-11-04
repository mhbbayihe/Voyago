package cm.enspd.voyago.trip.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cm.enspd.voyago.trip.domain.Trip;
import cm.enspd.voyago.trip.domain.TripStatus;

public interface TripRepository extends JpaRepository<Trip, UUID> {

	String FETCH = "select t from Trip t join fetch t.route r join fetch r.departure d join fetch r.arrival a "
			+ "join fetch t.vehicle v join fetch v.company c ";

	@Query(FETCH + "where lower(d.city) = lower(:from) and lower(a.city) = lower(:to) "
			+ "and t.departureAt >= :start and t.departureAt < :end and t.status = :status order by t.departureAt")
	List<Trip> search(@Param("from") String from, @Param("to") String to, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end, @Param("status") TripStatus status);

	@Query(FETCH + "where t.id = :id")
	Optional<Trip> findDetailed(@Param("id") UUID id);

	@Query(FETCH + "where c.id = :companyId and t.departureAt >= :from order by t.departureAt")
	List<Trip> findUpcomingByCompany(@Param("companyId") UUID companyId, @Param("from") LocalDateTime from);

	@Query(FETCH + "where t.departureAt >= :from and t.departureAt < :to order by t.departureAt")
	List<Trip> findBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
