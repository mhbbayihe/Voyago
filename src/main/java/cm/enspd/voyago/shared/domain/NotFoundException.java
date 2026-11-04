package cm.enspd.voyago.shared.domain;

public class NotFoundException extends BusinessException {

	public NotFoundException(String what) {
		super("NOT_FOUND", what + " introuvable");
	}
}
