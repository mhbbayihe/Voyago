package cm.enspd.voyago.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cm.enspd.voyago.notification.domain.Notification;
import cm.enspd.voyago.notification.domain.NotificationSender;

/** Passerelle SMS simulée (F17, priorité C) : le message est journalisé et visible dans la boîte de démo. */
@Component
class SmsSender implements NotificationSender {

	private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

	@Override
	public Notification.Channel channel() {
		return Notification.Channel.SMS;
	}

	@Override
	public void send(Notification n, byte[] attachment) {
		log.info("[SMS simulé] à {} : {}", n.getRecipient(), n.getBody());
		n.markSimulated();
	}
}
