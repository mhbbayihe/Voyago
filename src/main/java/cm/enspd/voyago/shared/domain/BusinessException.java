package cm.enspd.voyago.shared.domain;

/** Violation d'une règle métier : remontée à l'utilisateur avec un message lisible. */
public class BusinessException extends RuntimeException {

	private final String code;

	public BusinessException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
