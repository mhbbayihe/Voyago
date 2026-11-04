package cm.enspd.voyago.trip.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Point d'embarquement : gare routière, gare ferroviaire ou aéroport. */
@Entity
@Table(name = "station")
public class Station {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String city;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransportMode mode;

	protected Station() {
	}

	public Station(String name, String city, TransportMode mode) {
		this.name = name;
		this.city = city;
		this.mode = mode;
	}

	public UUID getId() { return id; }
	public String getName() { return name; }
	public String getCity() { return city; }
	public TransportMode getMode() { return mode; }
}
