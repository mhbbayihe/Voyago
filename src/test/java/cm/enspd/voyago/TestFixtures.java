package cm.enspd.voyago;

import java.time.LocalDateTime;
import java.util.UUID;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.auth.application.AuthService;
import cm.enspd.voyago.auth.domain.Role;
import cm.enspd.voyago.trip.application.CatalogService;
import cm.enspd.voyago.trip.domain.Company;
import cm.enspd.voyago.trip.domain.Route;
import cm.enspd.voyago.trip.domain.ServiceClass;
import cm.enspd.voyago.trip.domain.Station;
import cm.enspd.voyago.trip.domain.TransportMode;
import cm.enspd.voyago.trip.domain.Trip;
import cm.enspd.voyago.trip.domain.Vehicle;

/** Création d'un petit catalogue et de comptes pour les tests d'intégration. */
public final class TestFixtures {

	public record Fixture(Company company, Trip trip, AppUserPrincipal client, AppUserPrincipal agent) {
	}

	private TestFixtures() {
	}

	public static Fixture create(CatalogService catalog, AuthService auth, LocalDateTime departure) {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		Station a = catalog.createStation("Gare A " + suffix, "Douala", TransportMode.BUS);
		Station b = catalog.createStation("Gare B " + suffix, "Yaoundé", TransportMode.BUS);
		Route route = catalog.createRoute(a.getId(), b.getId(), 240, 240);
		Company company = catalog.createCompany("Compagnie " + suffix, TransportMode.BUS, null, null);
		Vehicle vehicle = catalog.createVehicle(company.getId(), "LT " + suffix, "Autocar", ServiceClass.STANDARD, 40, 4);
		Trip trip = catalog.scheduleTrip(null, route.getId(), vehicle.getId(), departure, 4000);
		var client = AppUserPrincipal.of(auth.registerClient("Client " + suffix, "client-" + suffix + "@test.cm",
				"677000000", "Password1"));
		var agent = AppUserPrincipal.of(auth.createStaff("Agent " + suffix, "agent-" + suffix + "@test.cm", null,
				"Password1", Role.AGENT, company.getId()));
		return new Fixture(company, trip, client, agent);
	}
}
