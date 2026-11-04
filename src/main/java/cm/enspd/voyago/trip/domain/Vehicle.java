package cm.enspd.voyago.trip.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Autocar, voiture de train ou avion. Les sièges sont numérotés de 1 à seatCount. */
@Entity
@Table(name = "vehicle")
public class Vehicle {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "company_id")
	private Company company;

	/** Plaque d'immatriculation, numéro de rame ou immatriculation de l'appareil. */
	@Column(nullable = false, unique = true)
	private String registration;

	@Column(nullable = false)
	private String model;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransportMode mode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ServiceClass serviceClass;

	@Column(nullable = false)
	private int seatCount;

	/** Nombre de sièges par rangée, pour dessiner le plan (ex. 4 = 2+2, 3 = 2+1, 6 = 3+3). */
	@Column(nullable = false)
	private int seatsPerRow;

	protected Vehicle() {
	}

	public Vehicle(Company company, String registration, String model, TransportMode mode, ServiceClass serviceClass,
			int seatCount, int seatsPerRow) {
		this.company = company;
		this.registration = registration;
		this.model = model;
		this.mode = mode;
		this.serviceClass = serviceClass;
		this.seatCount = seatCount;
		this.seatsPerRow = seatsPerRow;
	}

	public String getServiceClassLabel() {
		return serviceClass.labelFor(mode);
	}

	public UUID getId() { return id; }
	public Company getCompany() { return company; }
	public String getRegistration() { return registration; }
	public String getModel() { return model; }
	public TransportMode getMode() { return mode; }
	public ServiceClass getServiceClass() { return serviceClass; }
	public int getSeatCount() { return seatCount; }
	public int getSeatsPerRow() { return seatsPerRow; }
}
