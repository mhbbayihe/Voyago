package cm.enspd.voyago.payment.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cm.enspd.voyago.payment.domain.Payment;
import cm.enspd.voyago.payment.domain.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	Optional<Payment> findByIdempotencyKey(String key);

	Optional<Payment> findByExternalReference(String externalReference);

	List<Payment> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);

	List<Payment> findByBookingIdAndStatus(UUID bookingId, PaymentStatus status);

	List<Payment> findAllByOrderByCreatedAtDesc();
}
