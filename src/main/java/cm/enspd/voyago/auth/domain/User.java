package cm.enspd.voyago.auth.domain;

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
@Table(name = "app_user")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String email;

	private String phone;

	@Column(nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	/** Compagnie de rattachement d'un agent (RG7) ; null pour les clients et administrateurs. */
	private UUID companyId;

	private boolean active = true;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected User() {
	}

	public User(String fullName, String email, String phone, String passwordHash, Role role, UUID companyId) {
		this.fullName = fullName;
		this.email = email.toLowerCase().trim();
		this.phone = phone;
		this.passwordHash = passwordHash;
		this.role = role;
		this.companyId = companyId;
	}

	public void updateProfile(String fullName, String phone) {
		this.fullName = fullName;
		this.phone = phone;
	}

	public void changePasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	/** Initiales pour l'avatar (ex. « Client Démo » → « CD »). */
	public String getInitials() {
		String[] parts = fullName.trim().split("\\s+");
		String first = parts[0].substring(0, 1);
		String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
		return (first + last).toUpperCase();
	}

	public UUID getId() { return id; }
	public String getFullName() { return fullName; }
	public String getEmail() { return email; }
	public String getPhone() { return phone; }
	public String getPasswordHash() { return passwordHash; }
	public Role getRole() { return role; }
	public UUID getCompanyId() { return companyId; }
	public boolean isActive() { return active; }
	public LocalDateTime getCreatedAt() { return createdAt; }

	public void setActive(boolean active) { this.active = active; }
}
