package cm.enspd.voyago.shared.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.auth.application.AuthService;
import cm.enspd.voyago.auth.domain.Role;
import cm.enspd.voyago.auth.domain.User;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.application.BookingService.Customer;
import cm.enspd.voyago.booking.application.BookingService.PassengerInput;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.payment.application.PaymentService;
import cm.enspd.voyago.payment.domain.Payment;
import cm.enspd.voyago.payment.domain.PaymentMethod;
import cm.enspd.voyago.trip.application.CatalogService;
import cm.enspd.voyago.trip.application.TripSearchCriteria;
import cm.enspd.voyago.trip.application.TripService;
import cm.enspd.voyago.trip.application.TripView;
import cm.enspd.voyago.trip.domain.Company;
import cm.enspd.voyago.trip.domain.Route;
import cm.enspd.voyago.trip.domain.ServiceClass;
import cm.enspd.voyago.trip.domain.Station;
import cm.enspd.voyago.trip.domain.TransportMode;
import cm.enspd.voyago.trip.domain.Vehicle;

/**
 * Jeu de données de démonstration (exécuté au premier démarrage seulement, base vide).
 * Les compagnies sont fictives. Tout passe par les services applicatifs, donc par les mêmes
 * règles métier que l'application réelle.
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
	private static final int DAYS = 10;

	private final boolean enabled;
	private final AuthService auth;
	private final CatalogService catalog;
	private final TripService trips;
	private final BookingService bookings;
	private final PaymentService payments;
	private final Random random = new Random(15);

	public DemoDataInitializer(@Value("${voyago.seed-demo-data}") boolean enabled, AuthService auth,
			CatalogService catalog, TripService trips, BookingService bookings, PaymentService payments) {
		this.enabled = enabled;
		this.auth = auth;
		this.catalog = catalog;
		this.trips = trips;
		this.bookings = bookings;
		this.payments = payments;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!enabled || auth.count() > 0) {
			return;
		}
		log.info("Base vide : chargement des données de démonstration VoyaGo…");

		// ---------------------------------------------------------------- Points d'embarquement
		Map<String, Station> bus = new HashMap<>();
		station(bus, "Douala", "Gare routière de Mboppi", TransportMode.BUS);
		station(bus, "Yaoundé", "Gare routière de Mvan", TransportMode.BUS);
		station(bus, "Bafoussam", "Gare routière de Bafoussam", TransportMode.BUS);
		station(bus, "Kribi", "Gare routière de Kribi", TransportMode.BUS);
		station(bus, "Limbé", "Gare routière Mile 4", TransportMode.BUS);
		station(bus, "Bamenda", "Gare routière de Bamenda", TransportMode.BUS);
		station(bus, "Dschang", "Gare routière de Dschang", TransportMode.BUS);

		Map<String, Station> rail = new HashMap<>();
		station(rail, "Douala", "Gare de Bessengue", TransportMode.TRAIN);
		station(rail, "Yaoundé", "Gare centrale de Yaoundé", TransportMode.TRAIN);
		station(rail, "Bélabo", "Gare de Bélabo", TransportMode.TRAIN);
		station(rail, "Ngaoundéré", "Gare de Ngaoundéré", TransportMode.TRAIN);

		Map<String, Station> air = new HashMap<>();
		station(air, "Douala", "Aéroport international de Douala", TransportMode.PLANE);
		station(air, "Yaoundé", "Aéroport de Yaoundé-Nsimalen", TransportMode.PLANE);
		station(air, "Garoua", "Aéroport de Garoua", TransportMode.PLANE);
		station(air, "Maroua", "Aéroport de Maroua-Salak", TransportMode.PLANE);

		// ---------------------------------------------------------------- Lignes (aller et retour)
		List<Route> busRoutes = new ArrayList<>();
		both(busRoutes, bus, "Douala", "Yaoundé", 240, 240);
		both(busRoutes, bus, "Douala", "Bafoussam", 260, 300);
		both(busRoutes, bus, "Yaoundé", "Bafoussam", 290, 330);
		both(busRoutes, bus, "Douala", "Kribi", 160, 180);
		both(busRoutes, bus, "Douala", "Limbé", 75, 90);
		both(busRoutes, bus, "Douala", "Bamenda", 360, 420);
		both(busRoutes, bus, "Bafoussam", "Dschang", 50, 75);
		both(busRoutes, bus, "Yaoundé", "Kribi", 270, 300);

		List<Route> railRoutes = new ArrayList<>();
		both(railRoutes, rail, "Douala", "Yaoundé", 263, 225);
		both(railRoutes, rail, "Yaoundé", "Bélabo", 300, 360);
		both(railRoutes, rail, "Yaoundé", "Ngaoundéré", 622, 840);

		List<Route> airRoutes = new ArrayList<>();
		both(airRoutes, air, "Douala", "Yaoundé", 200, 45);
		both(airRoutes, air, "Douala", "Garoua", 760, 90);
		both(airRoutes, air, "Yaoundé", "Garoua", 560, 75);
		both(airRoutes, air, "Douala", "Maroua", 950, 110);
		both(airRoutes, air, "Yaoundé", "Maroua", 800, 100);

		// ---------------------------------------------------------------- Compagnies (fictives) et véhicules
		Company sanaga = catalog.createCompany("Sanaga Express", TransportMode.BUS, "+237 690 00 00 01", "contact@sanaga-express.cm");
		Company wouri = catalog.createCompany("Wouri Voyages", TransportMode.BUS, "+237 690 00 00 02", "contact@wouri-voyages.cm");
		Company fako = catalog.createCompany("Mont Fako Transport", TransportMode.BUS, "+237 690 00 00 03", "contact@montfako.cm");
		Company railCo = catalog.createCompany("Rail Cameroun Intercity", TransportMode.TRAIN, "+237 690 00 00 04", "contact@rail-intercity.cm");
		Company airCo = catalog.createCompany("Air Mungo", TransportMode.PLANE, "+237 690 00 00 05", "contact@airmungo.cm");

		List<Company> busCompanies = List.of(sanaga, wouri, fako);
		List<List<Vehicle>> busFleet = new ArrayList<>();
		String[] plates = { "LT", "CE", "SW" };
		for (int i = 0; i < busCompanies.size(); i++) {
			Company c = busCompanies.get(i);
			busFleet.add(List.of(
					catalog.createVehicle(c.getId(), plates[i] + " 1" + i + "4 AB", "Yutong ZK6122 (70 places)", ServiceClass.STANDARD, 40, 4),
					catalog.createVehicle(c.getId(), plates[i] + " 2" + i + "7 CD", "Mercedes Tourismo", ServiceClass.STANDARD, 40, 4),
					catalog.createVehicle(c.getId(), plates[i] + " 3" + i + "1 VP", "Scania Touring VIP", ServiceClass.PREMIUM, 30, 3)));
		}
		Vehicle train2nd = catalog.createVehicle(railCo.getId(), "IC-201", "Voiture Intercity 2e classe", ServiceClass.STANDARD, 64, 4);
		Vehicle train1st = catalog.createVehicle(railCo.getId(), "IC-101", "Voiture Intercity 1re classe", ServiceClass.PREMIUM, 36, 3);
		Vehicle planeEco = catalog.createVehicle(airCo.getId(), "TJ-AMA", "ATR 72-600 (Économique)", ServiceClass.STANDARD, 66, 6);
		Vehicle planeBiz = catalog.createVehicle(airCo.getId(), "TJ-AMB", "Embraer 145 (Affaires)", ServiceClass.PREMIUM, 12, 3);

		// ---------------------------------------------------------------- Voyages sur 10 jours
		LocalDate today = LocalDate.now();
		int[][] busHours = { { 6, 0, 13, 0 }, { 7, 30, 15, 30 }, { 10, 0, 19, 0 } };
		int count = 0;
		for (int d = 0; d < DAYS; d++) {
			LocalDate day = today.plusDays(d);
			for (Route r : busRoutes) {
				long classic = round(r.getDistanceKm() * 16, 500, 2000);
				for (int c = 0; c < busCompanies.size(); c++) {
					int[] h = busHours[c];
					count += schedule(r, busFleet.get(c).get(0), day, h[0], h[1], classic);
					Vehicle second = c == 0 ? busFleet.get(c).get(2) : busFleet.get(c).get(1);
					count += schedule(r, second, day, h[2], h[3], second.getServiceClass() == ServiceClass.PREMIUM
							? round(classic * 7 / 4, 500, 3000) : classic + 500);
				}
			}
			for (Route r : railRoutes) {
				long second = round(Math.round(r.getDistanceKm() * 11.5), 500, 2000);
				count += schedule(r, train2nd, day, 7, 10, second);
				count += schedule(r, train1st, day, 7, 10, round(second * 19 / 10, 500, 3000));
			}
			for (int i = 0; i < airRoutes.size(); i++) {
				Route r = airRoutes.get(i);
				long eco = round(40000 + r.getDistanceKm() * 35L, 1000, 40000);
				count += schedule(r, planeEco, day, 7 + (i % 5) * 2, i % 2 == 0 ? 15 : 45, eco);
				if (i < 4) {
					count += schedule(r, planeBiz, day, 17 + (i % 2), 30, round(eco * 5 / 2, 1000, 90000));
				}
			}
		}

		// ---------------------------------------------------------------- Comptes de démonstration
		auth.createStaff("Administrateur VoyaGo", "admin@voyago.cm", "+237 699 00 00 00", "Admin@2026", Role.ADMIN, null);
		auth.createStaff("Agent Sanaga Express", "agent@sanaga.cm", "+237 677 00 00 01", "Agent@2026", Role.AGENT, sanaga.getId());
		auth.createStaff("Agent Wouri Voyages", "agent@wouri.cm", "+237 677 00 00 02", "Agent@2026", Role.AGENT, wouri.getId());
		auth.createStaff("Agent Rail Intercity", "agent@rail.cm", "+237 677 00 00 04", "Agent@2026", Role.AGENT, railCo.getId());
		auth.createStaff("Agent Air Mungo", "agent@airmungo.cm", "+237 677 00 00 05", "Agent@2026", Role.AGENT, airCo.getId());
		User demo = auth.registerClient("Client Démo", "client@voyago.cm", "677123456", "Client@2026");

		String[][] people = { { "Marie", "Ngo Bassa" }, { "Paul", "Tchamba" }, { "Christine", "Mballa" },
				{ "Eric", "Fotso" }, { "Sylvie", "Abena" }, { "Divine", "Nkemelu" }, { "Arnaud", "Djoumessi" },
				{ "Grace", "Ewane" }, { "Ibrahim", "Hamadou" }, { "Laure", "Moukoko" } };
		List<User> clients = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			String[] p = people[i];
			clients.add(auth.registerClient(p[0] + " " + p[1], p[0].toLowerCase() + "@exemple.cm", "6771000" + (10 + i),
					"Client@2026"));
		}

		// ---------------------------------------------------------------- Réservations d'exemple (statistiques, scan)
		int booked = 0;
		booked += sampleBooking(demo, "Douala", "Yaoundé", TransportMode.BUS, today.plusDays(1), people, 2, true);
		booked += sampleBooking(demo, "Douala", "Garoua", TransportMode.PLANE, today.plusDays(4), people, 1, true);
		String[][] routes = { { "Douala", "Yaoundé" }, { "Yaoundé", "Douala" }, { "Douala", "Bafoussam" },
				{ "Douala", "Kribi" }, { "Yaoundé", "Bafoussam" }, { "Douala", "Limbé" } };
		for (int i = 0; i < 22; i++) {
			User u = clients.get(i % clients.size());
			String[] r = routes[i % routes.length];
			TransportMode mode = i % 7 == 3 ? TransportMode.TRAIN : (i % 9 == 5 ? TransportMode.PLANE : TransportMode.BUS);
			booked += sampleBooking(u, r[0], r[1], mode, today.plusDays(1 + i % 6), people, 1 + i % 3, i % 8 != 7);
		}
		log.info("Données de démonstration chargées : {} voyages, {} réservations.", count, booked);
		log.info("Comptes : admin@voyago.cm / Admin@2026 · agent@sanaga.cm / Agent@2026 · client@voyago.cm / Client@2026");
	}

	private void station(Map<String, Station> map, String city, String name, TransportMode mode) {
		map.put(city, catalog.createStation(name, city, mode));
	}

	private void both(List<Route> list, Map<String, Station> stations, String a, String b, int km, int minutes) {
		list.add(catalog.createRoute(stations.get(a).getId(), stations.get(b).getId(), km, minutes));
		list.add(catalog.createRoute(stations.get(b).getId(), stations.get(a).getId(), km, minutes));
	}

	private int schedule(Route route, Vehicle vehicle, LocalDate day, int hour, int minute, long price) {
		LocalDateTime departure = day.atTime(LocalTime.of(hour, minute));
		if (departure.isBefore(LocalDateTime.now().plusMinutes(30))) {
			return 0;
		}
		catalog.scheduleTrip(null, route.getId(), vehicle.getId(), departure, price);
		return 1;
	}

	private static long round(long value, int step, long min) {
		return Math.max(min, Math.round((double) value / step) * step);
	}

	/** Réserve puis paie via le simulateur Mobile Money (même parcours qu'un vrai client). */
	private int sampleBooking(User user, String from, String to, TransportMode mode, LocalDate day, String[][] people,
			int passengers, boolean keep) {
		List<TripView> found = trips.search(new TripSearchCriteria(from, to, day, passengers, mode, null, null, null, null, null));
		if (found.isEmpty()) {
			return 0;
		}
		TripView trip = found.get(random.nextInt(found.size()));
		List<Integer> free = new ArrayList<>();
		var map = trips.seatMap(trip.id());
		for (int s = 1; s <= map.seatCount(); s++) {
			if (!map.isOccupied(s)) {
				free.add(s);
			}
		}
		Collections.shuffle(free, random);
		List<PassengerInput> list = new ArrayList<>();
		for (int i = 0; i < passengers && i < free.size(); i++) {
			String[] p = people[random.nextInt(people.length)];
			list.add(new PassengerInput(p[0], p[1], "CNI" + (100000000 + random.nextInt(899999999)), free.get(i)));
		}
		AppUserPrincipal principal = AppUserPrincipal.of(user);
		Booking booking = bookings.create(Customer.of(principal), trip.id(), list, null);
		PaymentMethod method = random.nextBoolean() ? PaymentMethod.MTN_MOMO : PaymentMethod.ORANGE_MONEY;
		String phone = method == PaymentMethod.MTN_MOMO ? "677" + (100000 + random.nextInt(899999))
				: "699" + (100000 + random.nextInt(899999));
		Payment payment = payments.initiate(principal, booking.getId(), method, phone, UUID.randomUUID().toString());
		payments.handleProviderResult(method, payment.getExternalReference(), true, null);
		if (!keep) {
			bookings.cancel(principal, booking.getId());
		}
		return 1;
	}
}
