package cm.enspd.voyago.auth.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/** Émission et vérification des jetons JWT de l'API REST (durée courte : 15 min). */
@Service
public class JwtService {

	private final SecretKey key;
	private final long validityMinutes;

	public JwtService(@Value("${voyago.security.jwt-secret}") String secret,
			@Value("${voyago.security.jwt-validity-minutes}") long validityMinutes) {
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
		this.validityMinutes = validityMinutes;
	}

	public String generate(AppUserPrincipal user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.email())
				.claim("uid", user.id().toString())
				.claim("role", user.role().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(validityMinutes, ChronoUnit.MINUTES)))
				.signWith(key)
				.compact();
	}

	/** @return l'e-mail (sujet) du jeton s'il est valide et non expiré. */
	public String validateAndGetSubject(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
	}

	public long getValiditySeconds() {
		return validityMinutes * 60;
	}
}
