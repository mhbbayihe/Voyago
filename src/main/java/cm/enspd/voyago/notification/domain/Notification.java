package cm.enspd.voyago.notification.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification", indexes = @Index(name = "idx_notification_user", columnList = "userId"))
public class Notification {

	public enum Channel {
		EMAIL("E-mail"), SMS("SMS");

		private final String label;

		Channel(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	public enum Status {
		/** Envoyé par SMTP / passerelle SMS. */
		SENT,
		/** Mode démonstration : conservé dans la boîte de réception VoyaGo, pas d'envoi réel. */
		SIMULATED,
		FAILED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Channel channel;

	@Column(nullable = false)
	private String recipient;

	private String subject;

	@Column(nullable = false, length = 4000)
	private String body;

	private String attachmentName;

	private UUID bookingId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.SIMULATED;

	private String error;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected Notification() {
	}

	public Notification(UUID userId, Channel channel, String recipient, String subject, String body, UUID bookingId) {
		this.userId = userId;
		this.channel = channel;
		this.recipient = recipient;
		this.subject = subject;
		this.body = body;
		this.bookingId = bookingId;
	}

	public void attach(String name) {
		this.attachmentName = name;
	}

	public void markSent() {
		status = Status.SENT;
	}

	public void markSimulated() {
		status = Status.SIMULATED;
	}

	public void markFailed(String error) {
		status = Status.FAILED;
		this.error = error;
	}

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public Channel getChannel() { return channel; }
	public String getRecipient() { return recipient; }
	public String getSubject() { return subject; }
	public String getBody() { return body; }
	public String getAttachmentName() { return attachmentName; }
	public UUID getBookingId() { return bookingId; }
	public Status getStatus() { return status; }
	public String getError() { return error; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
