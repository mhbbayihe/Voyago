package cm.enspd.voyago.auth.application;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import cm.enspd.voyago.auth.infrastructure.UserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public AppUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		return users.findByEmail(email.toLowerCase().trim())
				.map(AppUserPrincipal::of)
				.orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu"));
	}
}
