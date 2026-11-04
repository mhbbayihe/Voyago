package cm.enspd.voyago.auth.application;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import cm.enspd.voyago.auth.domain.Role;
import cm.enspd.voyago.auth.domain.User;

/** Utilisateur authentifié, partagé par la session web et le jeton JWT. */
public record AppUserPrincipal(UUID id, String email, String fullName, String phone, String passwordHash,
		Role role, UUID companyId, boolean active) implements UserDetails {

	public static AppUserPrincipal of(User u) {
		return new AppUserPrincipal(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getPasswordHash(),
				u.getRole(), u.getCompanyId(), u.isActive());
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

	public boolean isAgent() {
		return role == Role.AGENT;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}
}
