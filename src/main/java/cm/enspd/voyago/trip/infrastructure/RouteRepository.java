package cm.enspd.voyago.trip.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cm.enspd.voyago.trip.domain.Route;

public interface RouteRepository extends JpaRepository<Route, UUID> {

	@Query("select r from Route r join fetch r.departure d join fetch r.arrival a order by d.mode, d.city, a.city")
	List<Route> findAllOrdered();
}
