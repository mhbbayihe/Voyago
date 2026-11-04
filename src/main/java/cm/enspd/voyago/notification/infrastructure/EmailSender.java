package cm.enspd.voyago.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import cm.enspd.voyago.notification.domain.Notification;
import cm.enspd.voyago.notification.domain.NotificationSender;
import jakarta.mail.internet.MimeMessage;

/**
 * Envoi d'e-mails. Avec voyago.mail.enabled=true, envoi réel par SMTP (Mailpit en développement) ;
 * sinon l'e-mail est conservé dans la boîte de réception de démonstration de l'application.
 */
@Component
class EmailSender implements NotificationSender {

	private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

	private final JavaMailSender mailSender;
	private final boolean enabled;
	private final String from;

	EmailSender(JavaMailSender mailSender, @Value("${voyago.mail.enabled}") boolean enabled,
			@Value("${voyago.mail.from}") String from) {
		this.mailSender = mailSender;
		this.enabled = enabled;
		this.from = from;
	}

	@Override
	public Notification.Channel channel() {
		return Notification.Channel.EMAIL;
	}

	@Override
	public void send(Notification n, byte[] attachment) {
		if (!enabled) {
			log.info("[E-MAIL simulé] à {} : {}", n.getRecipient(), n.getSubject());
			n.markSimulated();
			return;
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
			helper.setFrom(from);
			helper.setTo(n.getRecipient());
			helper.setSubject(n.getSubject());
			helper.setText(n.getBody());
			if (attachment != null) {
				helper.addAttachment(n.getAttachmentName(), new ByteArrayResource(attachment), "application/pdf");
			}
			mailSender.send(message);
			n.markSent();
		} catch (Exception e) {
			log.warn("Échec d'envoi de l'e-mail à {} : {}", n.getRecipient(), e.getMessage());
			n.markFailed(e.getMessage());
		}
	}
}
