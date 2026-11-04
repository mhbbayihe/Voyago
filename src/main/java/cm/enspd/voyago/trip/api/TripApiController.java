package cm.enspd.voyago.trip.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.trip.application.CatalogService;
import cm.enspd.voyago.trip.application.SeatMapView;
import cm.enspd.voyago.trip.application.TripSearchCriteria;
import cm.enspd.voyago.trip.application.TripService;
import cm.enspd.voyago.trip.application.TripView;
import cm.enspd.voyago.trip.domain.ServiceClass;
import cm.enspd.voyago.trip.domain.TransportMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@Tag(name = "Voyages")
public class TripApiController {

	private final TripService trips;
	private final CatalogService catalog;

	public TripApiController(TripService trips, CatalogService catalog) {
		this.trips = trips;
		this.catalog = catalog;
	}

	@GetMapping("/api/v1/cities")
	@Operation(summary = "Villes desservies")
	public List<String> cities() {
		return trips.cities();
	}

	@GetMapping("/api/v1/trips")
	@Operation(summary = "Rechercher des voyages (F06)")
	public List<TripView> search(@RequestParam String from, @RequestParam String to,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(defaultValue = "1") int passengers, @RequestParam(required = false) TransportMode mode,
			@RequestParam(required = false) UUID companyId, @RequestParam(required = false) Long maxPrice,
			@RequestParam(required = false) String period, @RequestParam(required = false) ServiceClass serviceClass,
			@RequestParam(defaultValue = "DEPARTURE") String sort) {
		return trips.search(new TripSearchCriteria(from, to, date, passengers, mode, companyId, maxPrice, period,
				serviceClass, sort));
	}

	@GetMapping("/api/v1/trips/{id}")
	@Operation(summary = "Détail d'un voyage")
	public TripView trip(@PathVariable UUID id) {
		return trips.view(id);
	}

	@GetMapping("/api/v1/trips/{id}/seats")
	@Operation(summary = "Plan et disponibilité des sièges (F07)")
	public SeatMapView seats(@PathVariable UUID id) {
		return trips.seatMap(id);
	}

	public record ScheduleTripRequest(@NotNull UUID routeId, @NotNull UUID vehicleId, @NotNull LocalDateTime departureAt,
			@Positive long price) {
	}

	@PostMapping("/api/v1/admin/trips")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Programmer un voyage (agent : uniquement les véhicules de sa compagnie)")
	public TripView schedule(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody ScheduleTripRequest req) {
		UUID companyScope = me.isAdmin() ? null : me.companyId();
		var trip = catalog.scheduleTrip(companyScope, req.routeId(), req.vehicleId(), req.departureAt(), req.price());
		return trips.view(trip.getId());
	}
}
