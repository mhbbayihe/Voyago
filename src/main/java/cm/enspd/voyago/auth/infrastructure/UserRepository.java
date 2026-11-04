package cm.enspd.voyago.auth.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cm.enspd.voyago.auth.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	List<User> findAllByOrderByCreatedAtDesc();
}
