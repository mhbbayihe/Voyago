package cm.enspd.voyago.shared.domain;

/** Accès refusé à une ressource qui n'appartient pas à l'utilisateur (RG7). */
public class ForbiddenException extends BusinessException {

	public ForbiddenException(String message) {
		super("FORBIDDEN", message);
	}
}
