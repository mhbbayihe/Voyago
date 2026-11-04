package cm.enspd.voyago.notification.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cm.enspd.voyago.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

	List<Notification> findTop100ByOrderByCreatedAtDesc();
}
