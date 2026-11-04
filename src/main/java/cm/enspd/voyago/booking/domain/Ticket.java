package cm.enspd.voyago.booking.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.trip.domain.TransportMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Billet électronique : un par passager, contrôlable une seule fois (RG6). */
@Entity
@Table(name = "ticket", indexes = @Index(name = "idx_ticket_trip", columnList = "tripId"))
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(nullable = false)
	private UUID bookingId;

	@Column(nullable = false)
	private String bookingReference;

	@Column(nullable = false)
	private String passengerName;

	@Column(nullable = false)
	private String passengerIdNumber;

	@Column(nullable = false)
	private int seatNumber;

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
	private long price;

	/** Signature HMAC du code, intégrée au QR code pour empêcher la falsification. */
	@Column(nullable = false)
	private String signature;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TicketStatus status = TicketStatus.VALID;

	@Column(nullable = false)
	private LocalDateTime issuedAt;

	private LocalDateTime usedAt;

	@Version
	private long version;

	protected Ticket() {
	}

	public Ticket(String code, String signature, Booking booking, Passenger passenger, LocalDateTime now) {
		this.code = code;
		this.signature = signature;
		this.bookingId = booking.getId();
		this.bookingReference = booking.getReference();
		this.passengerName = passenger.getFullName();
		this.passengerIdNumber = passenger.getIdNumber();
		this.seatNumber = passenger.getSeatNumber();
		this.tripId = booking.getTripId();
		this.companyId = booking.getCompanyId();
		this.companyName = booking.getCompanyName();
		this.mode = booking.getMode();
		this.tripLabel = booking.getTripLabel();
		this.fromStation = booking.getFromStation();
		this.toStation = booking.getToStation();
		this.serviceClassLabel = booking.getServiceClassLabel();
		this.departureAt = booking.getDepartureAt();
		this.price = booking.getUnitPrice();
		this.issuedAt = now;
	}

	/** RG6 : un billet scanné devient « utilisé » et ne peut plus être validé. */
	public void markUsed(LocalDateTime now) {
		if (status != TicketStatus.VALID) {
			throw new BusinessException("TICKET_NOT_VALID", "Billet " + status.getLabel().toLowerCase());
		}
		status = TicketStatus.USED;
		usedAt = now;
	}

	public void cancel() {
		if (status == TicketStatus.USED) {
			throw new BusinessException("TICKET_USED", "Un billet déjà utilisé ne peut pas être annulé");
		}
		status = TicketStatus.CANCELLED;
	}

	public UUID getId() { return id; }
	public String getCode() { return code; }
	public UUID getBookingId() { return bookingId; }
	public String getBookingReference() { return bookingReference; }
	public String getPassengerName() { return passengerName; }
	public String getPassengerIdNumber() { return passengerIdNumber; }
	public int getSeatNumber() { return seatNumber; }
	public UUID getTripId() { return tripId; }
	public UUID getCompanyId() { return companyId; }
	public String getCompanyName() { return companyName; }
	public TransportMode getMode() { return mode; }
	public String getTripLabel() { return tripLabel; }
	public String getFromStation() { return fromStation; }
	public String getToStation() { return toStation; }
	public String getServiceClassLabel() { return serviceClassLabel; }
	public LocalDateTime getDepartureAt() { return departureAt; }
	public long getPrice() { return price; }
	public String getSignature() { return signature; }
	public TicketStatus getStatus() { return status; }
	public LocalDateTime getIssuedAt() { return issuedAt; }
	public LocalDateTime getUsedAt() { return usedAt; }
}
