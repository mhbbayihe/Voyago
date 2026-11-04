package cm.enspd.voyago.auth.application;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.enspd.voyago.auth.domain.Role;
import cm.enspd.voyago.auth.domain.User;
import cm.enspd.voyago.auth.infrastructure.UserRepository;
import cm.enspd.voyago.shared.audit.AuditService;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.NotFoundException;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AuditService audit;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, AuditService audit) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.audit = audit;
	}

	@Transactional
	public User registerClient(String fullName, String email, String phone, String rawPassword) {
		return create(fullName, email, phone, rawPassword, Role.CLIENT, null);
	}

	@Transactional
	public User createStaff(String fullName, String email, String phone, String rawPassword, Role role, UUID companyId) {
		if (role == Role.AGENT && companyId == null) {
			throw new BusinessException("COMPANY_REQUIRED", "Un agent doit être rattaché à une compagnie");
		}
		User user = create(fullName, email, phone, rawPassword, role, role == Role.AGENT ? companyId : null);
		audit.record("CREATION_COMPTE", role + " " + user.getEmail());
		return user;
	}

	private User create(String fullName, String email, String phone, String rawPassword, Role role, UUID companyId) {
		String normalized = email.toLowerCase().trim();
		if (users.existsByEmail(normalized)) {
			throw new BusinessException("EMAIL_TAKEN", "Un compte existe déjà avec cet e-mail");
		}
		if (rawPassword == null || rawPassword.length() < 8) {
			throw new BusinessException("WEAK_PASSWORD", "Le mot de passe doit contenir au moins 8 caractères");
		}
		return users.save(new User(fullName.trim(), normalized, phone, passwordEncoder.encode(rawPassword), role, companyId));
	}

	@Transactional
	public void setActive(UUID userId, boolean active) {
		User user = get(userId);
		user.setActive(active);
		audit.record(active ? "ACTIVATION_COMPTE" : "DESACTIVATION_COMPTE", user.getEmail());
	}

	@Transactional
	public User updateProfile(UUID userId, String fullName, String phone) {
		if (fullName == null || fullName.isBlank()) {
			throw new BusinessException("NAME_REQUIRED", "Le nom complet est obligatoire");
		}
		User user = get(userId);
		user.updateProfile(fullName.trim(), phone == null ? null : phone.trim());
		return user;
	}

	@Transactional
	public void changePassword(UUID userId, String currentPassword, String newPassword, String confirmation) {
		User user = get(userId);
		if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw new BusinessException("BAD_PASSWORD", "Le mot de passe actuel est incorrect");
		}
		if (newPassword == null || newPassword.length() < 8) {
			throw new BusinessException("WEAK_PASSWORD", "Le nouveau mot de passe doit contenir au moins 8 caractères");
		}
		if (!newPassword.equals(confirmation)) {
			throw new BusinessException("PASSWORD_MISMATCH", "La confirmation ne correspond pas au nouveau mot de passe");
		}
		user.changePasswordHash(passwordEncoder.encode(newPassword));
		audit.record("CHANGEMENT_MOT_DE_PASSE", user.getEmail());
	}

	public User get(UUID id) {
		return users.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur"));
	}

	public List<User> all() {
		return users.findAllByOrderByCreatedAtDesc();
	}

	public long count() {
		return users.count();
	}
}
