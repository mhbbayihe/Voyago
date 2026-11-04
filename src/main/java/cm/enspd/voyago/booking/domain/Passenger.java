package cm.enspd.voyago.booking.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** RG3 : chaque passager fournit nom, prénom et pièce d'identité. */
@Entity
@Table(name = "passenger")
public class Passenger {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "booking_id")
	private Booking booking;

	@Column(nullable = false)
	private String firstName;

	@Column(nullable = false)
	private String lastName;

	@Column(nullable = false)
	private String idNumber;

	@Column(nullable = false)
	private int seatNumber;

	protected Passenger() {
	}

	Passenger(Booking booking, String firstName, String lastName, String idNumber, int seatNumber) {
		this.booking = booking;
		this.firstName = firstName.trim();
		this.lastName = lastName.trim().toUpperCase();
		this.idNumber = idNumber.trim().toUpperCase();
		this.seatNumber = seatNumber;
	}

	public String getFullName() {
		return lastName + " " + firstName;
	}

	public UUID getId() { return id; }
	public Booking getBooking() { return booking; }
	public String getFirstName() { return firstName; }
	public String getLastName() { return lastName; }
	public String getIdNumber() { return idNumber; }
	public int getSeatNumber() { return seatNumber; }
}
