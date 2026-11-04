package cm.enspd.voyago.payment.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Vérifie que le webhook provient bien du fournisseur : HMAC-SHA256(secret, "référence|statut") en hexadécimal. */
@Component
public class WebhookSignature {

	private final SecretKeySpec key;

	public WebhookSignature(@Value("${voyago.security.webhook-secret}") String secret) {
		this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	public String compute(String externalReference, String status) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(key);
			return HexFormat.of().formatHex(mac.doFinal((externalReference + "|" + status).getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public boolean isValid(String externalReference, String status, String signature) {
		if (signature == null) {
			return false;
		}
		return MessageDigest.isEqual(compute(externalReference, status).getBytes(StandardCharsets.UTF_8),
				signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
	}
}
