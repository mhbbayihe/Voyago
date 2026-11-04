package cm.enspd.voyago.trip.domain;

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

/** Compagnie de transport (autocars, chemin de fer ou compagnie aérienne). */
@Entity
@Table(name = "company")
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransportMode mode;

	private String phone;

	private String email;

	private boolean active = true;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected Company() {
	}

	public Company(String name, TransportMode mode, String phone, String email) {
		this.name = name;
		this.mode = mode;
		this.phone = phone;
		this.email = email;
	}

	public UUID getId() { return id; }
	public String getName() { return name; }
	public TransportMode getMode() { return mode; }
	public String getPhone() { return phone; }
	public String getEmail() { return email; }
	public boolean isActive() { return active; }
	public LocalDateTime getCreatedAt() { return createdAt; }

	public void setActive(boolean active) { this.active = active; }
}
