package cm.enspd.voyago.trip.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cm.enspd.voyago.trip.domain.Company;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

	List<Company> findAllByOrderByNameAsc();

	List<Company> findByActiveTrueOrderByNameAsc();

	boolean existsByNameIgnoreCase(String name);
}
