package cm.enspd.voyago.shared.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Événements d'intégration échangés entre modules (pattern Observer / Publish-Subscribe).
 * Ils jouent le rôle des messages RabbitMQ de l'architecture microservices cible : aucun module
 * n'appelle directement un autre pour réagir à un fait métier, il publie un événement.
 * Ensemble, ils forment la saga chorégraphiée réservation ↔ paiement.
 */
public final class DomainEvents {

	private DomainEvents() {
	}

	/** Payment → Booking : le fournisseur a confirmé le paiement. */
	public record PaymentConfirmed(UUID paymentId, UUID bookingId, long amount, String method) {
	}

	/** Payment → Notification : le paiement a été refusé ou a échoué. */
	public record PaymentFailed(UUID paymentId, UUID bookingId, UUID customerId, String customerEmail, String reason) {
	}

	/** Booking → Notification : réservation confirmée, billets émis. */
	public record BookingConfirmed(UUID bookingId, String reference, UUID customerId, String customerEmail,
			String customerName, String customerPhone, String tripLabel, LocalDateTime departureAt, long totalAmount) {
	}

	/** Booking → Payment (remboursement) et Notification. */
	public record BookingCancelled(UUID bookingId, String reference, UUID customerId, String customerEmail,
			String customerName, String tripLabel, boolean wasPaid, long refundAmount) {
	}

	/** Booking → Payment (annulation des paiements en attente) et Notification : verrou expiré (RG2). */
	public record BookingExpired(UUID bookingId, String reference, UUID customerId, String customerEmail,
			String customerName, String tripLabel) {
	}

	/** Booking → Payment : paiement reçu alors que la réservation n'est plus valable, compensation par remboursement. */
	public record LatePaymentRefundRequested(UUID bookingId, UUID paymentId, long amount) {
	}

	/** Booking → Notification : rappel avant le départ (F16). */
	public record DepartureReminder(UUID bookingId, String reference, UUID customerId, String customerEmail,
			String customerName, String customerPhone, String tripLabel, LocalDateTime departureAt) {
	}
}
