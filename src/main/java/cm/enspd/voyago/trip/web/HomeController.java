package cm.enspd.voyago.trip.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cm.enspd.voyago.trip.application.CatalogService;
import cm.enspd.voyago.trip.application.TripSearchCriteria;
import cm.enspd.voyago.trip.application.TripService;
import cm.enspd.voyago.trip.application.TripView;
import cm.enspd.voyago.trip.domain.ServiceClass;
import cm.enspd.voyago.trip.domain.TransportMode;

@Controller
public class HomeController {

	private final TripService trips;
	private final CatalogService catalog;

	public HomeController(TripService trips, CatalogService catalog) {
		this.trips = trips;
		this.catalog = catalog;
	}

	/** Trajet mis en avant sur l'accueil ; fromPrice = prix le plus bas trouvé pour demain (null si aucun). */
	public record PopularRoute(String from, String to, TransportMode mode, Long fromPrice) {
	}

	@GetMapping("/")
	public String home(Model model) {
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		List<PopularRoute> popular = List.of(
				popular("Douala", "Yaoundé", TransportMode.BUS, tomorrow),
				popular("Douala", "Yaoundé", TransportMode.TRAIN, tomorrow),
				popular("Douala", "Garoua", TransportMode.PLANE, tomorrow),
				popular("Yaoundé", "Bafoussam", TransportMode.BUS, tomorrow),
				popular("Yaoundé", "Ngaoundéré", TransportMode.TRAIN, tomorrow),
				popular("Douala", "Kribi", TransportMode.BUS, tomorrow));
		model.addAttribute("cities", trips.cities());
		model.addAttribute("today", LocalDate.now());
		model.addAttribute("tomorrow", tomorrow);
		model.addAttribute("popular", popular);
		return "index";
	}

	private PopularRoute popular(String from, String to, TransportMode mode, LocalDate day) {
		Long min = trips.search(new TripSearchCriteria(from, to, day, 1, mode, null, null, null, null, "PRICE")).stream()
				.map(TripView::price).findFirst().orElse(null);
		return new PopularRoute(from, to, mode, min);
	}

	@GetMapping("/search")
	public String search(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(defaultValue = "1") int passengers, @RequestParam(required = false) TransportMode mode,
			@RequestParam(required = false) UUID companyId, @RequestParam(required = false) Long maxPrice,
			@RequestParam(required = false) String period, @RequestParam(required = false) ServiceClass serviceClass,
			@RequestParam(defaultValue = "DEPARTURE") String sort, Model model) {
		LocalDate day = date == null ? LocalDate.now() : date;
		var criteria = new TripSearchCriteria(from, to, day, passengers, mode, companyId, maxPrice, period, serviceClass, sort);
		List<TripView> results = trips.search(criteria);
		model.addAttribute("criteria", criteria);
		model.addAttribute("results", results);
		model.addAttribute("cities", trips.cities());
		model.addAttribute("companies", catalog.activeCompanies());
		model.addAttribute("days", List.of(day.minusDays(1), day, day.plusDays(1), day.plusDays(2)));
		model.addAttribute("today", LocalDate.now());
		return "trips/search";
	}

	@GetMapping("/about")
	public String about() {
		return "about";
	}
}
