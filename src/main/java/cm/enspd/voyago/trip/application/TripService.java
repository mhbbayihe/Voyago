package cm.enspd.voyago.trip.application;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.NotFoundException;
import cm.enspd.voyago.trip.domain.Trip;
import cm.enspd.voyago.trip.domain.TripStatus;
import cm.enspd.voyago.trip.domain.port.SeatOccupancyPort;
import cm.enspd.voyago.trip.infrastructure.StationRepository;
import cm.enspd.voyago.trip.infrastructure.TripRepository;

/** Cas d'utilisation de consultation : recherche de voyages et disponibilité des sièges. */
@Service
@Transactional(readOnly = true)
public class TripService {

	private final TripRepository trips;
	private final StationRepository stations;
	private final SeatOccupancyPort occupancy;

	public TripService(TripRepository trips, StationRepository stations, SeatOccupancyPort occupancy) {
		this.trips = trips;
		this.stations = stations;
		this.occupancy = occupancy;
	}

	public List<String> cities() {
		return stations.findDistinctCities();
	}

	/** F06 / F07 / RG8 : recherche filtrée, sans les voyages passés ou complets. */
	public List<TripView> search(TripSearchCriteria c) {
		if (c.from() == null || c.to() == null || c.date() == null) {
			return List.of();
		}
		LocalDateTime start = c.date().atStartOfDay();
		List<Trip> found = trips.search(c.from(), c.to(), start, start.plusDays(1), TripStatus.SCHEDULED);
		LocalDateTime now = LocalDateTime.now();
		int passengers = Math.max(1, c.passengers());

		Stream<TripView> views = toViews(found.stream().filter(t -> t.isBookable(now)).toList()).stream()
				.filter(v -> v.availableSeats() >= passengers)
				.filter(v -> c.mode() == null || v.mode() == c.mode())
				.filter(v -> c.companyId() == null || v.companyId().equals(c.companyId()))
				.filter(v -> c.maxPrice() == null || v.price() <= c.maxPrice())
				.filter(v -> c.serviceClass() == null || v.serviceClass() == c.serviceClass())
				.filter(v -> matchesPeriod(v, c.period()));

		Comparator<TripView> order = switch (c.sort() == null ? "" : c.sort()) {
			case "PRICE" -> Comparator.comparingLong(TripView::price);
			case "DURATION" -> Comparator.comparingInt(TripView::durationMinutes);
			default -> Comparator.comparing(TripView::departureAt);
		};
		return views.sorted(order).toList();
	}

	private boolean matchesPeriod(TripView v, String period) {
		if (period == null || period.isBlank()) {
			return true;
		}
		int hour = v.departureAt().getHour();
		return switch (period) {
			case "MORNING" -> hour < 12;
			case "AFTERNOON" -> hour >= 12 && hour < 18;
			case "EVENING" -> hour >= 18;
			default -> true;
		};
	}

	public TripView view(UUID tripId) {
		Trip trip = trips.findDetailed(tripId).orElseThrow(() -> new NotFoundException("Voyage"));
		return toViews(List.of(trip)).get(0);
	}

	public SeatMapView seatMap(UUID tripId) {
		Trip trip = trips.findDetailed(tripId).orElseThrow(() -> new NotFoundException("Voyage"));
		return SeatMapView.build(trip.getVehicle().getSeatCount(), trip.getVehicle().getSeatsPerRow(),
				occupancy.occupiedSeats(tripId));
	}

	/** Utilisé par le module booking avant de créer une réservation (RG8). */
	public TripView requireBookable(UUID tripId) {
		Trip trip = trips.findDetailed(tripId).orElseThrow(() -> new NotFoundException("Voyage"));
		if (!trip.isBookable(LocalDateTime.now())) {
			throw new BusinessException("TRIP_NOT_BOOKABLE", "Ce voyage n'est plus disponible à la réservation");
		}
		return toViews(List.of(trip)).get(0);
	}

	public List<TripView> upcomingForCompany(UUID companyId) {
		return toViews(trips.findUpcomingByCompany(companyId, LocalDateTime.now().minusHours(6)));
	}

	public List<TripView> between(LocalDateTime from, LocalDateTime to) {
		return toViews(trips.findBetween(from, to));
	}

	private List<TripView> toViews(List<Trip> list) {
		if (list.isEmpty()) {
			return List.of();
		}
		Map<UUID, Long> occupied = occupancy.occupiedCount(list.stream().map(Trip::getId).toList());
		return list.stream().map(t -> TripView.of(t, occupied.getOrDefault(t.getId(), 0L))).toList();
	}
}
