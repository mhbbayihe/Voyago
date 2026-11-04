package cm.enspd.voyago.payment.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cm.enspd.voyago.payment.domain.Refund;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

	List<Refund> findAllByOrderByCreatedAtDesc();
}
