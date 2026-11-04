package cm.enspd.voyago.payment.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refund")
public class Refund {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID paymentId;

	@Column(nullable = false)
	private String bookingReference;

	@Column(nullable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentMethod method;

	private String externalReference;

	private String reason;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected Refund() {
	}

	public Refund(Payment payment, long amount, String externalReference, String reason) {
		this.paymentId = payment.getId();
		this.bookingReference = payment.getBookingReference();
		this.amount = amount;
		this.method = payment.getMethod();
		this.externalReference = externalReference;
		this.reason = reason;
	}

	public UUID getId() { return id; }
	public UUID getPaymentId() { return paymentId; }
	public String getBookingReference() { return bookingReference; }
	public long getAmount() { return amount; }
	public PaymentMethod getMethod() { return method; }
	public String getExternalReference() { return externalReference; }
	public String getReason() { return reason; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
