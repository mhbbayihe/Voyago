package cm.enspd.voyago.shared.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** RG9 : trace des actions sensibles. */
@Entity
@Table(name = "audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String actor;

	@Column(nullable = false)
	private String action;

	@Column(length = 1000)
	private String details;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected AuditLog() {
	}

	public AuditLog(String actor, String action, String details) {
		this.actor = actor;
		this.action = action;
		this.details = details;
	}

	public UUID getId() { return id; }
	public String getActor() { return actor; }
	public String getAction() { return action; }
	public String getDetails() { return details; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
