package cm.enspd.voyago.booking.domain;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.trip.domain.TransportMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Agrégat Réservation. Les informations du voyage et du client sont recopiées (instantané) :
 * le module booking ne lit jamais les tables des autres modules, comme dans une architecture à services.
 */
@Entity
@Table(name = "booking", indexes = { @Index(name = "idx_booking_customer", columnList = "customerId"),
		@Index(name = "idx_booking_status_expiry", columnList = "status, expiresAt") })
public class Booking {

	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String reference;

	@Column(nullable = false)
	private UUID customerId;

	@Column(nullable = false)
	private String customerName;

	@Column(nullable = false)
	private String customerEmail;

	private String customerPhone;

	@Column(nullable = false)
	private UUID tripId;

	@Column(nullable = false)
	private UUID companyId;

	@Column(nullable = false)
	private String companyName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransportMode mode;

	@Column(nullable = false)
	private String tripLabel;

	private String fromStation;

	private String toStation;

	private String serviceClassLabel;

	@Column(nullable = false)
	private LocalDateTime departureAt;

	@Column(nullable = false)
	private long unitPrice;

	@Column(nullable = false)
	private long totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookingStatus status = BookingStatus.PENDING_PAYMENT;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	/** RG2 : date d'expiration du verrou des sièges. */
	@Column(nullable = false)
	private LocalDateTime expiresAt;

	private LocalDateTime confirmedAt;

	private LocalDateTime cancelledAt;

	private long refundAmount;

	private boolean reminderSent;

	/** Vente au guichet (F20) : identifiant de l'agent vendeur. */
	private UUID soldByAgentId;

	@Version
	private long version;

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("seatNumber")
	private List<Passenger> passengers = new ArrayList<>();

	protected Booking() {
	}

	public static Booking create(UUID customerId, String customerName, String customerEmail, String customerPhone,
			BookedTrip trip, LocalDateTime now, int lockMinutes) {
		Booking b = new Booking();
		b.reference = "VG-" + randomCode(6);
		b.customerId = customerId;
		b.customerName = customerName;
		b.customerEmail = customerEmail;
		b.customerPhone = customerPhone;
		b.tripId = trip.tripId();
		b.companyId = trip.companyId();
		b.companyName = trip.companyName();
		b.mode = trip.mode();
		b.tripLabel = trip.label();
		b.fromStation = trip.fromStation();
		b.toStation = trip.toStation();
		b.serviceClassLabel = trip.serviceClassLabel();
		b.departureAt = trip.departureAt();
		b.unitPrice = trip.unitPrice();
		b.createdAt = now;
		b.expiresAt = now.plusMinutes(lockMinutes);
		return b;
	}

	public static String randomCode(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}

	public void addPassenger(String firstName, String lastName, String idNumber, int seatNumber) {
		passengers.add(new Passenger(this, firstName, lastName, idNumber, seatNumber));
		totalAmount = unitPrice * passengers.size();
	}

	public void markSoldBy(UUID agentId) {
		this.soldByAgentId = agentId;
	}

	public void confirm(LocalDateTime now) {
		transitionTo(BookingStatus.CONFIRMED);
		confirmedAt = now;
	}

	public void expire() {
		transitionTo(BookingStatus.EXPIRED);
	}

	public void cancel(long refund, LocalDateTime now) {
		transitionTo(BookingStatus.CANCELLED);
		refundAmount = refund;
		cancelledAt = now;
	}

	public void markReminderSent() {
		reminderSent = true;
	}

	private void transitionTo(BookingStatus target) {
		if (!status.canTransitionTo(target)) {
			throw new BusinessException("INVALID_TRANSITION",
					"Impossible de passer la réservation de « " + status.getLabel() + " » à « " + target.getLabel() + " »");
		}
		status = target;
	}

	public boolean isLockExpired(LocalDateTime now) {
		return status == BookingStatus.PENDING_PAYMENT && expiresAt.isBefore(now);
	}

	public boolean isPayable(LocalDateTime now) {
		return status == BookingStatus.PENDING_PAYMENT && !isLockExpired(now);
	}

	public boolean isOwnedBy(UUID userId) {
		return customerId.equals(userId);
	}

	public String getSeatsLabel() {
		return String.join(", ", passengers.stream().map(p -> String.valueOf(p.getSeatNumber())).toList());
	}

	/** Montant effectivement encaissé (après éventuel remboursement). */
	public long getRetainedAmount() {
		if (status == BookingStatus.CONFIRMED) {
			return totalAmount;
		}
		if (status == BookingStatus.CANCELLED && confirmedAt != null) {
			return totalAmount - refundAmount;
		}
		return 0;
	}

	public UUID getId() { return id; }
	public String getReference() { return reference; }
	public UUID getCustomerId() { return customerId; }
	public String getCustomerName() { return customerName; }
	public String getCustomerEmail() { return customerEmail; }
	public String getCustomerPhone() { return customerPhone; }
	public UUID getTripId() { return tripId; }
	public UUID getCompanyId() { return companyId; }
	public String getCompanyName() { return companyName; }
	public TransportMode getMode() { return mode; }
	public String getTripLabel() { return tripLabel; }
	public String getFromStation() { return fromStation; }
	public String getToStation() { return toStation; }
	public String getServiceClassLabel() { return serviceClassLabel; }
	public LocalDateTime getDepartureAt() { return departureAt; }
	public long getUnitPrice() { return unitPrice; }
	public long getTotalAmount() { return totalAmount; }
	public BookingStatus getStatus() { return status; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getExpiresAt() { return expiresAt; }
	public LocalDateTime getConfirmedAt() { return confirmedAt; }
	public LocalDateTime getCancelledAt() { return cancelledAt; }
	public long getRefundAmount() { return refundAmount; }
	public boolean isReminderSent() { return reminderSent; }
	public UUID getSoldByAgentId() { return soldByAgentId; }
	public List<Passenger> getPassengers() { return passengers; }
}
