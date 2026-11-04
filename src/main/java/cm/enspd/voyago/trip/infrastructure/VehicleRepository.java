package cm.enspd.voyago.trip.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cm.enspd.voyago.trip.domain.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

	List<Vehicle> findByCompanyIdOrderByRegistrationAsc(UUID companyId);

	boolean existsByRegistrationIgnoreCase(String registration);
}
