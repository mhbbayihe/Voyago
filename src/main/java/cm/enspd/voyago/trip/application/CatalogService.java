package cm.enspd.voyago.trip.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.enspd.voyago.shared.audit.AuditService;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.ForbiddenException;
import cm.enspd.voyago.shared.domain.NotFoundException;
import cm.enspd.voyago.trip.domain.Company;
import cm.enspd.voyago.trip.domain.Route;
import cm.enspd.voyago.trip.domain.ServiceClass;
import cm.enspd.voyago.trip.domain.Station;
import cm.enspd.voyago.trip.domain.TransportMode;
import cm.enspd.voyago.trip.domain.Trip;
import cm.enspd.voyago.trip.domain.Vehicle;
import cm.enspd.voyago.trip.infrastructure.CompanyRepository;
import cm.enspd.voyago.trip.infrastructure.RouteRepository;
import cm.enspd.voyago.trip.infrastructure.StationRepository;
import cm.enspd.voyago.trip.infrastructure.TripRepository;
import cm.enspd.voyago.trip.infrastructure.VehicleRepository;

/** Cas d'utilisation de gestion du catalogue (F04, F05) : compagnies, gares, lignes, véhicules, voyages. */
@Service
@Transactional
public class CatalogService {

	private final CompanyRepository companies;
	private final StationRepository stations;
	private final RouteRepository routes;
	private final VehicleRepository vehicles;
	private final TripRepository trips;
	private final AuditService audit;

	public CatalogService(CompanyRepository companies, StationRepository stations, RouteRepository routes,
			VehicleRepository vehicles, TripRepository trips, AuditService audit) {
		this.companies = companies;
		this.stations = stations;
		this.routes = routes;
		this.vehicles = vehicles;
		this.trips = trips;
		this.audit = audit;
	}

	// ---- Compagnies

	@Transactional(readOnly = true)
	public List<Company> companies() {
		return companies.findAllByOrderByNameAsc();
	}

	@Transactional(readOnly = true)
	public List<Company> activeCompanies() {
		return companies.findByActiveTrueOrderByNameAsc();
	}

	@Transactional(readOnly = true)
	public Company company(UUID id) {
		return companies.findById(id).orElseThrow(() -> new NotFoundException("Compagnie"));
	}

	public Company createCompany(String name, TransportMode mode, String phone, String email) {
		if (companies.existsByNameIgnoreCase(name.trim())) {
			throw new BusinessException("COMPANY_EXISTS", "Cette compagnie existe déjà");
		}
		Company company = companies.save(new Company(name.trim(), mode, phone, email));
		audit.record("CREATION_COMPAGNIE", company.getName());
		return company;
	}

	public void setCompanyActive(UUID id, boolean active) {
		Company company = company(id);
		company.setActive(active);
		audit.record(active ? "ACTIVATION_COMPAGNIE" : "SUSPENSION_COMPAGNIE", company.getName());
	}

	// ---- Gares et lignes

	@Transactional(readOnly = true)
	public List<Station> stations() {
		return stations.findAllByOrderByCityAscNameAsc();
	}

	public Station createStation(String name, String city, TransportMode mode) {
		return stations.save(new Station(name.trim(), city.trim(), mode));
	}

	@Transactional(readOnly = true)
	public List<Route> routes() {
		return routes.findAllOrdered();
	}

	public Route createRoute(UUID departureId, UUID arrivalId, int distanceKm, int durationMinutes) {
		if (departureId.equals(arrivalId)) {
			throw new BusinessException("SAME_STATION", "Le départ et l'arrivée doivent être différents");
		}
		Station from = stations.findById(departureId).orElseThrow(() -> new NotFoundException("Gare de départ"));
		Station to = stations.findById(arrivalId).orElseThrow(() -> new NotFoundException("Gare d'arrivée"));
		return routes.save(new Route(from, to, distanceKm, durationMinutes));
	}

	// ---- Véhicules

	@Transactional(readOnly = true)
	public List<Vehicle> vehicles(UUID companyId) {
		return vehicles.findByCompanyIdOrderByRegistrationAsc(companyId);
	}

	public Vehicle createVehicle(UUID companyId, String registration, String model, ServiceClass serviceClass,
			int seatCount, int seatsPerRow) {
		Company company = company(companyId);
		if (vehicles.existsByRegistrationIgnoreCase(registration.trim())) {
			throw new BusinessException("VEHICLE_EXISTS", "Un véhicule porte déjà cette immatriculation");
		}
		if (seatCount < 1 || seatCount > 300 || seatsPerRow < 1 || seatsPerRow > 8) {
			throw new BusinessException("INVALID_SEATS", "Nombre de sièges invalide");
		}
		return vehicles.save(new Vehicle(company, registration.trim().toUpperCase(), model.trim(), company.getMode(),
				serviceClass, seatCount, seatsPerRow));
	}

	// ---- Voyages

	/**
	 * F05 : programmation d'un voyage. RG7 : si {@code agentCompanyId} n'est pas null, le véhicule
	 * doit appartenir à la compagnie de l'agent.
	 */
	public Trip scheduleTrip(UUID agentCompanyId, UUID routeId, UUID vehicleId, LocalDateTime departureAt, long price) {
		Route route = routes.findById(routeId).orElseThrow(() -> new NotFoundException("Ligne"));
		Vehicle vehicle = vehicles.findById(vehicleId).orElseThrow(() -> new NotFoundException("Véhicule"));
		if (agentCompanyId != null && !vehicle.getCompany().getId().equals(agentCompanyId)) {
			throw new ForbiddenException("Ce véhicule n'appartient pas à votre compagnie");
		}
		if (departureAt == null || departureAt.isBefore(LocalDateTime.now())) {
			throw new BusinessException("PAST_DEPARTURE", "La date de départ doit être dans le futur");
		}
		Trip trip = trips.save(new Trip(route, vehicle, departureAt, price));
		audit.record("PROGRAMMATION_VOYAGE", route.getLabel() + " le " + departureAt + " (" + price + " XAF)");
		return trip;
	}

	/** RG9 : toute modification de tarif est tracée. */
	public void changePrice(UUID agentCompanyId, UUID tripId, long newPrice) {
		Trip trip = ownedTrip(agentCompanyId, tripId);
		long old = trip.getPrice();
		trip.changePrice(newPrice);
		audit.record("MODIFICATION_TARIF", trip.getRoute().getLabel() + " " + trip.getDepartureAt() + " : " + old
				+ " → " + newPrice + " XAF");
	}

	private Trip ownedTrip(UUID agentCompanyId, UUID tripId) {
		Trip trip = trips.findDetailed(tripId).orElseThrow(() -> new NotFoundException("Voyage"));
		if (agentCompanyId != null && !trip.getCompany().getId().equals(agentCompanyId)) {
			throw new ForbiddenException("Ce voyage n'appartient pas à votre compagnie");
		}
		return trip;
	}
}
