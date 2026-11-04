package cm.enspd.voyago.booking.web;

import java.util.ArrayList;
import java.util.List;

import cm.enspd.voyago.booking.application.BookingService.PassengerInput;

/** Formulaire « sièges + passagers » (liaison de données Spring MVC). */
public class BookingForm {

	private List<PassengerForm> passengers = new ArrayList<>();

	/** Vente au guichet : identité du client présent. */
	private String customerName;
	private String customerPhone;

	public static class PassengerForm {
		private Integer seatNumber;
		private String firstName;
		private String lastName;
		private String idNumber;

		public Integer getSeatNumber() { return seatNumber; }
		public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
		public String getFirstName() { return firstName; }
		public void setFirstName(String firstName) { this.firstName = firstName; }
		public String getLastName() { return lastName; }
		public void setLastName(String lastName) { this.lastName = lastName; }
		public String getIdNumber() { return idNumber; }
		public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
	}

	public List<PassengerInput> toInputs() {
		return passengers.stream().filter(p -> p.getSeatNumber() != null)
				.map(p -> new PassengerInput(p.getFirstName(), p.getLastName(), p.getIdNumber(), p.getSeatNumber()))
				.toList();
	}

	public List<PassengerForm> getPassengers() { return passengers; }
	public void setPassengers(List<PassengerForm> passengers) { this.passengers = passengers; }
	public String getCustomerName() { return customerName; }
	public void setCustomerName(String customerName) { this.customerName = customerName; }
	public String getCustomerPhone() { return customerPhone; }
	public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
}
