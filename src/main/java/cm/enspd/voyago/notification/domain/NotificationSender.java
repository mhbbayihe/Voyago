package cm.enspd.voyago.notification.domain;

/** Port de sortie : un canal d'envoi (e-mail, SMS…). */
public interface NotificationSender {

	Notification.Channel channel();

	/** Envoie la notification et met à jour son statut. */
	void send(Notification notification, byte[] attachment);
}
