package cm.enspd.voyago.notification.application;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import cm.enspd.voyago.notification.domain.Notification;
import cm.enspd.voyago.notification.domain.Notification.Channel;
import cm.enspd.voyago.notification.domain.NotificationSender;

/** Pattern Factory : crée la notification et fournit l'expéditeur adapté au canal. */
@Component
public class NotificationFactory {

	private final Map<Channel, NotificationSender> senders = new EnumMap<>(Channel.class);

	public NotificationFactory(List<NotificationSender> all) {
		all.forEach(s -> senders.put(s.channel(), s));
	}

	public Notification email(UUID userId, String email, String subject, String body, UUID bookingId) {
		return new Notification(userId, Channel.EMAIL, email, subject, body, bookingId);
	}

	public Notification sms(UUID userId, String phone, String body, UUID bookingId) {
		return new Notification(userId, Channel.SMS, phone, null, body, bookingId);
	}

	public NotificationSender senderFor(Channel channel) {
		NotificationSender sender = senders.get(channel);
		if (sender == null) {
			throw new IllegalStateException("Aucun expéditeur pour le canal " + channel);
		}
		return sender;
	}
}
