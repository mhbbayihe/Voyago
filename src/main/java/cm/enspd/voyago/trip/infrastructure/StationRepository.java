package cm.enspd.voyago.trip.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cm.enspd.voyago.trip.domain.Station;

public interface StationRepository extends JpaRepository<Station, UUID> {

	List<Station> findAllByOrderByCityAscNameAsc();

	@Query("select distinct s.city from Station s order by s.city")
	List<String> findDistinctCities();
}
