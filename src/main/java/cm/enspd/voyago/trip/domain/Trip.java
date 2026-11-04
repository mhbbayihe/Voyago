package cm.enspd.voyago.trip.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import cm.enspd.voyago.shared.domain.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Voyage programmé : une ligne, un véhicule, une date et un prix (en XAF, entier). */
@Entity
@Table(name = "trip", indexes = @Index(name = "idx_trip_departure", columnList = "departureAt"))
public class Trip {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "route_id")
	private Route route;

	@ManyToOne(optional = false)
	@JoinColumn(name = "vehicle_id")
	private Vehicle vehicle;

	@Column(nullable = false)
	private LocalDateTime departureAt;

	@Column(nullable = false)
	private LocalDateTime arrivalAt;

	@Column(nullable = false)
	private long price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TripStatus status = TripStatus.SCHEDULED;

	@Version
	private long version;

	protected Trip() {
	}

	public Trip(Route route, Vehicle vehicle, LocalDateTime departureAt, long price) {
		if (route.getMode() != vehicle.getMode()) {
			throw new BusinessException("MODE_MISMATCH", "Le véhicule ne correspond pas au mode de transport de la ligne");
		}
		if (price <= 0) {
			throw new BusinessException("INVALID_PRICE", "Le prix doit être positif");
		}
		this.route = route;
		this.vehicle = vehicle;
		this.departureAt = departureAt;
		this.arrivalAt = departureAt.plusMinutes(route.getDurationMinutes());
		this.price = price;
	}

	/** RG8 : un voyage passé ou annulé n'est plus réservable. */
	public boolean isBookable(LocalDateTime now) {
		return status == TripStatus.SCHEDULED && departureAt.isAfter(now);
	}

	public void changePrice(long newPrice) {
		if (newPrice <= 0) {
			throw new BusinessException("INVALID_PRICE", "Le prix doit être positif");
		}
		this.price = newPrice;
	}

	public void cancel() {
		this.status = TripStatus.CANCELLED;
	}

	public Company getCompany() {
		return vehicle.getCompany();
	}

	public TransportMode getMode() {
		return vehicle.getMode();
	}

	public UUID getId() { return id; }
	public Route getRoute() { return route; }
	public Vehicle getVehicle() { return vehicle; }
	public LocalDateTime getDepartureAt() { return departureAt; }
	public LocalDateTime getArrivalAt() { return arrivalAt; }
	public long getPrice() { return price; }
	public TripStatus getStatus() { return status; }
}
