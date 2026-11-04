package cm.enspd.voyago.payment.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import cm.enspd.voyago.shared.domain.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Transaction de paiement. Aucune donnée de compte Mobile Money n'est stockée : seulement
 * le numéro payeur et la référence externe du fournisseur. RG4 : clé d'idempotence unique.
 */
@Entity
@Table(name = "payment", indexes = @Index(name = "idx_payment_booking", columnList = "bookingId"))
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID bookingId;

	@Column(nullable = false)
	private String bookingReference;

	@Column(nullable = false)
	private UUID customerId;

	private String customerEmail;

	@Column(nullable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentMethod method;

	private String payerPhone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status = PaymentStatus.PENDING;

	@Column(unique = true)
	private String externalReference;

	@Column(nullable = false, unique = true)
	private String idempotencyKey;

	private String failureReason;

	private long refundedAmount;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	private LocalDateTime updatedAt;

	@Version
	private long version;

	protected Payment() {
	}

	public Payment(UUID bookingId, String bookingReference, UUID customerId, String customerEmail, long amount,
			PaymentMethod method, String payerPhone, String idempotencyKey) {
		this.bookingId = bookingId;
		this.bookingReference = bookingReference;
		this.customerId = customerId;
		this.customerEmail = customerEmail;
		this.amount = amount;
		this.method = method;
		this.payerPhone = payerPhone;
		this.idempotencyKey = idempotencyKey;
	}

	public void initiated(String externalReference) {
		this.externalReference = externalReference;
		touch();
	}

	public void succeed() {
		requireStatus(PaymentStatus.PENDING);
		status = PaymentStatus.SUCCEEDED;
		touch();
	}

	public void fail(String reason) {
		requireStatus(PaymentStatus.PENDING);
		status = PaymentStatus.FAILED;
		failureReason = reason;
		touch();
	}

	public void cancel(String reason) {
		requireStatus(PaymentStatus.PENDING);
		status = PaymentStatus.CANCELLED;
		failureReason = reason;
		touch();
	}

	public void refund(long refundAmount) {
		if (status != PaymentStatus.SUCCEEDED && status != PaymentStatus.PARTIALLY_REFUNDED) {
			throw new BusinessException("NOT_REFUNDABLE", "Seul un paiement réussi peut être remboursé");
		}
		if (refundedAmount + refundAmount > amount) {
			throw new BusinessException("REFUND_TOO_HIGH", "Le remboursement dépasse le montant payé");
		}
		refundedAmount += refundAmount;
		status = refundedAmount == amount ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
		touch();
	}

	private void requireStatus(PaymentStatus expected) {
		if (status != expected) {
			throw new BusinessException("INVALID_PAYMENT_STATE", "Paiement déjà traité (" + status.getLabel() + ")");
		}
	}

	private void touch() {
		updatedAt = LocalDateTime.now();
	}

	public UUID getId() { return id; }
	public UUID getBookingId() { return bookingId; }
	public String getBookingReference() { return bookingReference; }
	public UUID getCustomerId() { return customerId; }
	public String getCustomerEmail() { return customerEmail; }
	public long getAmount() { return amount; }
	public PaymentMethod getMethod() { return method; }
	public String getPayerPhone() { return payerPhone; }
	public PaymentStatus getStatus() { return status; }
	public String getExternalReference() { return externalReference; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public String getFailureReason() { return failureReason; }
	public long getRefundedAmount() { return refundedAmount; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
