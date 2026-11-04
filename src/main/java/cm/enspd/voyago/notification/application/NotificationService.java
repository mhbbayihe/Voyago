package cm.enspd.voyago.notification.application;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.application.TicketService;
import cm.enspd.voyago.notification.domain.Notification;
import cm.enspd.voyago.notification.infrastructure.NotificationRepository;
import cm.enspd.voyago.shared.events.DomainEvents.BookingCancelled;
import cm.enspd.voyago.shared.events.DomainEvents.BookingConfirmed;
import cm.enspd.voyago.shared.events.DomainEvents.BookingExpired;
import cm.enspd.voyago.shared.events.DomainEvents.DepartureReminder;
import cm.enspd.voyago.shared.events.DomainEvents.PaymentFailed;

/**
 * Abonné aux événements métier (F16, F17). Les envois ont lieu APRÈS la validation de la transaction :
 * un problème d'e-mail ne peut jamais annuler une réservation ou un paiement.
 */
@Service
public class NotificationService {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm", Locale.FRENCH);

	private final NotificationRepository notifications;
	private final NotificationFactory factory;
	private final BookingService bookings;
	private final TicketService tickets;

	public NotificationService(NotificationRepository notifications, NotificationFactory factory,
			BookingService bookings, TicketService tickets) {
		this.notifications = notifications;
		this.factory = factory;
		this.bookings = bookings;
		this.tickets = tickets;
	}

	@TransactionalEventListener(fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onBookingConfirmed(BookingConfirmed e) {
		byte[] pdf = tickets.pdf(bookings.tickets(e.bookingId()));
		Notification mail = factory.email(e.customerId(), e.customerEmail(), "Votre billet VoyaGo " + e.reference(),
				"Bonjour " + e.customerName() + ",\n\n"
						+ "Votre paiement est confirmé. Voici votre billet électronique pour le voyage " + e.tripLabel()
						+ " du " + DATE.format(e.departureAt()) + ".\n\n"
						+ "Référence : " + e.reference() + "\nMontant payé : " + money(e.totalAmount()) + "\n\n"
						+ "Présentez le QR code du billet (pièce jointe PDF) à l'embarquement, avec votre pièce d'identité.\n\n"
						+ "Bon voyage avec VoyaGo !",
				e.bookingId());
		mail.attach("billet-" + e.reference() + ".pdf");
		dispatch(mail, pdf);
		if (e.customerPhone() != null && !e.customerPhone().isBlank()) {
			dispatch(factory.sms(e.customerId(), e.customerPhone(), "VoyaGo : réservation " + e.reference()
					+ " confirmée. " + e.tripLabel() + " le " + DATE.format(e.departureAt()) + ".", e.bookingId()), null);
		}
	}

	@TransactionalEventListener(fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onBookingCancelled(BookingCancelled e) {
		String refund = e.wasPaid()
				? (e.refundAmount() > 0 ? "Un remboursement de " + money(e.refundAmount()) + " a été effectué sur votre compte Mobile Money."
						: "Selon le barème d'annulation, aucun remboursement n'est dû.")
				: "Aucun paiement n'avait été effectué.";
		dispatch(factory.email(e.customerId(), e.customerEmail(), "Annulation de la réservation " + e.reference(),
				"Bonjour " + e.customerName() + ",\n\nVotre réservation " + e.reference() + " (" + e.tripLabel()
						+ ") a bien été annulée.\n" + refund + "\n\nL'équipe VoyaGo",
				e.bookingId()), null);
	}

	@TransactionalEventListener(fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onBookingExpired(BookingExpired e) {
		dispatch(factory.email(e.customerId(), e.customerEmail(), "Réservation " + e.reference() + " expirée",
				"Bonjour " + e.customerName() + ",\n\nLe délai de paiement de votre réservation " + e.reference() + " ("
						+ e.tripLabel() + ") est dépassé : les sièges ont été libérés.\n"
						+ "Vous pouvez effectuer une nouvelle réservation à tout moment.\n\nL'équipe VoyaGo",
				e.bookingId()), null);
	}

	@TransactionalEventListener(fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onPaymentFailed(PaymentFailed e) {
		if (e.customerEmail() == null) {
			return;
		}
		dispatch(factory.email(e.customerId(), e.customerEmail(), "Paiement non abouti",
				"Bonjour,\n\nVotre paiement n'a pas abouti : " + e.reason() + ".\n"
						+ "Vos sièges restent réservés jusqu'à la fin du délai de 15 minutes : vous pouvez réessayer.\n\nL'équipe VoyaGo",
				e.bookingId()), null);
	}

	@TransactionalEventListener(fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onDepartureReminder(DepartureReminder e) {
		dispatch(factory.email(e.customerId(), e.customerEmail(), "Rappel : votre départ approche",
				"Bonjour " + e.customerName() + ",\n\nPetit rappel : votre voyage " + e.tripLabel() + " part le "
						+ DATE.format(e.departureAt()) + " (réservation " + e.reference() + ").\n"
						+ "Pensez à vous présenter 30 minutes avant le départ avec votre billet et votre pièce d'identité.\n\nL'équipe VoyaGo",
				e.bookingId()), null);
	}

	private void dispatch(Notification notification, byte[] attachment) {
		factory.senderFor(notification.getChannel()).send(notification, attachment);
		notifications.save(notification);
	}

	@Transactional(readOnly = true)
	public List<Notification> forUser(UUID userId) {
		return notifications.findByUserIdOrderByCreatedAtDesc(userId);
	}

	@Transactional(readOnly = true)
	public List<Notification> latest() {
		return notifications.findTop100ByOrderByCreatedAtDesc();
	}

	private static String money(long amount) {
		return String.format(Locale.FRENCH, "%,d FCFA", amount);
	}
}
