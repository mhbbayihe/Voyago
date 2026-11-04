package cm.enspd.voyago.trip.domain;

import java.util.UUID;

import cm.enspd.voyago.shared.domain.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Ligne reliant deux points d'embarquement du même mode de transport. */
@Entity
@Table(name = "route")
public class Route {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "departure_station_id")
	private Station departure;

	@ManyToOne(optional = false)
	@JoinColumn(name = "arrival_station_id")
	private Station arrival;

	@Column(nullable = false)
	private int distanceKm;

	@Column(nullable = false)
	private int durationMinutes;

	protected Route() {
	}

	public Route(Station departure, Station arrival, int distanceKm, int durationMinutes) {
		if (departure.getId() != null && departure.getId().equals(arrival.getId())) {
			throw new BusinessException("SAME_STATION", "Le départ et l'arrivée doivent être différents");
		}
		if (departure.getMode() != arrival.getMode()) {
			throw new BusinessException("MODE_MISMATCH", "Les deux points d'une ligne doivent être du même mode de transport");
		}
		this.departure = departure;
		this.arrival = arrival;
		this.distanceKm = distanceKm;
		this.durationMinutes = durationMinutes;
	}

	public TransportMode getMode() {
		return departure.getMode();
	}

	public String getLabel() {
		return departure.getCity() + " → " + arrival.getCity();
	}

	public UUID getId() { return id; }
	public Station getDeparture() { return departure; }
	public Station getArrival() { return arrival; }
	public int getDistanceKm() { return distanceKm; }
	public int getDurationMinutes() { return durationMinutes; }
}
