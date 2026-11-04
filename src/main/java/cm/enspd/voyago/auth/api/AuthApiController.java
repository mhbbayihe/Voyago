package cm.enspd.voyago.auth.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.auth.application.AuthService;
import cm.enspd.voyago.auth.application.JwtService;
import cm.enspd.voyago.auth.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification")
public class AuthApiController {

	private final AuthService auth;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwt;

	public AuthApiController(AuthService auth, AuthenticationManager authenticationManager, JwtService jwt) {
		this.auth = auth;
		this.authenticationManager = authenticationManager;
		this.jwt = jwt;
	}

	public record RegisterRequest(@NotBlank String fullName, @NotBlank @Email String email, String phone,
			@NotBlank @Size(min = 8) String password) {
	}

	public record LoginRequest(@NotBlank String email, @NotBlank String password) {
	}

	public record UserResponse(UUID id, String fullName, String email, String role) {
		static UserResponse of(User u) {
			return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name());
		}
	}

	public record TokenResponse(String accessToken, String tokenType, long expiresIn, String role) {
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Créer un compte client")
	public UserResponse register(@Valid @RequestBody RegisterRequest req) {
		return UserResponse.of(auth.registerClient(req.fullName(), req.email(), req.phone(), req.password()));
	}

	@PostMapping("/login")
	@Operation(summary = "S'authentifier et obtenir un jeton JWT (15 min)")
	public TokenResponse login(@Valid @RequestBody LoginRequest req) {
		Authentication result = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(req.email().toLowerCase().trim(), req.password()));
		AppUserPrincipal user = (AppUserPrincipal) result.getPrincipal();
		return new TokenResponse(jwt.generate(user), "Bearer", jwt.getValiditySeconds(), user.role().name());
	}
}
