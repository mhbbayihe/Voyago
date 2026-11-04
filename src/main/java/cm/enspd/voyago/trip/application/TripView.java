package cm.enspd.voyago.trip.application;

import java.time.LocalDateTime;
import java.util.UUID;

import cm.enspd.voyago.trip.domain.ServiceClass;
import cm.enspd.voyago.trip.domain.TransportMode;
import cm.enspd.voyago.trip.domain.Trip;
import cm.enspd.voyago.trip.domain.TripStatus;

/** Représentation d'un voyage exposée aux autres modules et à l'interface (DTO). */
public record TripView(UUID id, TransportMode mode, UUID companyId, String companyName, String fromCity,
		String fromStation, String toCity, String toStation, LocalDateTime departureAt, LocalDateTime arrivalAt,
		int durationMinutes, long price, ServiceClass serviceClass, String serviceClassLabel, String vehicleModel,
		String vehicleRegistration, int seatCount, int availableSeats, TripStatus status) {

	static TripView of(Trip t, long occupied) {
		var r = t.getRoute();
		var v = t.getVehicle();
		return new TripView(t.getId(), t.getMode(), t.getCompany().getId(), t.getCompany().getName(),
				r.getDeparture().getCity(), r.getDeparture().getName(), r.getArrival().getCity(), r.getArrival().getName(),
				t.getDepartureAt(), t.getArrivalAt(), r.getDurationMinutes(), t.getPrice(), v.getServiceClass(),
				v.getServiceClassLabel(), v.getModel(), v.getRegistration(), v.getSeatCount(),
				(int) Math.max(0, v.getSeatCount() - occupied), t.getStatus());
	}

	public String label() {
		return fromCity + " → " + toCity;
	}

	public String durationLabel() {
		return (durationMinutes / 60) + " h " + String.format("%02d", durationMinutes % 60);
	}

	public int occupancyPercent() {
		return seatCount == 0 ? 0 : (int) Math.round(100.0 * (seatCount - availableSeats) / seatCount);
	}

	public boolean full() {
		return availableSeats <= 0;
	}
}
