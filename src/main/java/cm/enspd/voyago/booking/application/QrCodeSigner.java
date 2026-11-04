package cm.enspd.voyago.booking.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Signature HMAC-SHA256 du contenu des QR codes. Format : « VOYAGO|code|signature ».
 * Sans la clé secrète, impossible de fabriquer un faux billet valide.
 */
@Component
public class QrCodeSigner {

	private static final String PREFIX = "VOYAGO";

	private final SecretKeySpec key;

	public QrCodeSigner(@Value("${voyago.security.qr-secret}") String secret) {
		this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	public String sign(String code) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(key);
			byte[] raw = mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(raw).substring(0, 22);
		} catch (Exception e) {
			throw new IllegalStateException("Signature impossible", e);
		}
	}

	public String payload(String code) {
		return PREFIX + "|" + code + "|" + sign(code);
	}

	public boolean isSignedPayload(String content) {
		return content != null && content.startsWith(PREFIX + "|");
	}

	/** @return le code du billet si la signature est correcte. */
	public Optional<String> verify(String content) {
		String[] parts = content.trim().split("\\|");
		if (parts.length != 3 || !PREFIX.equals(parts[0])) {
			return Optional.empty();
		}
		boolean ok = MessageDigest.isEqual(sign(parts[1]).getBytes(StandardCharsets.UTF_8),
				parts[2].getBytes(StandardCharsets.UTF_8));
		return ok ? Optional.of(parts[1]) : Optional.empty();
	}
}
