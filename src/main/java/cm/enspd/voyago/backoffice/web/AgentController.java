package cm.enspd.voyago.backoffice.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.backoffice.application.StatsService;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.application.BookingService.Customer;
import cm.enspd.voyago.booking.application.TicketService;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.booking.web.BookingForm;
import cm.enspd.voyago.payment.application.PaymentService;
import cm.enspd.voyago.payment.domain.PaymentMethod;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.ForbiddenException;
import cm.enspd.voyago.trip.application.CatalogService;
import cm.enspd.voyago.trip.application.TripService;
import cm.enspd.voyago.trip.application.TripView;
import cm.enspd.voyago.trip.domain.Company;
import cm.enspd.voyago.trip.domain.ServiceClass;

/** Back-office de l'agent de compagnie. RG7 : toutes les données sont filtrées sur sa compagnie. */
@Controller
@RequestMapping("/agent")
public class AgentController {

	private final CatalogService catalog;
	private final TripService trips;
	private final BookingService bookings;
	private final TicketService tickets;
	private final PaymentService payments;
	private final StatsService stats;

	public AgentController(CatalogService catalog, TripService trips, BookingService bookings, TicketService tickets,
			PaymentService payments, StatsService stats) {
		this.catalog = catalog;
		this.trips = trips;
		this.bookings = bookings;
		this.tickets = tickets;
		this.payments = payments;
		this.stats = stats;
	}

	@ModelAttribute("company")
	public Company company(@AuthenticationPrincipal AppUserPrincipal me) {
		return me == null || me.companyId() == null ? null : catalog.company(me.companyId());
	}

	@GetMapping
	public String dashboard(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
		List<TripView> upcoming = trips.upcomingForCompany(me.companyId());
		model.addAttribute("stats", stats.forCompany(me.companyId()));
		model.addAttribute("nextTrips", upcoming.stream().limit(8).toList());
		model.addAttribute("avgFill", (int) upcoming.stream().mapToInt(TripView::occupancyPercent).average().orElse(0));
		model.addAttribute("tripCount", upcoming.size());
		return "agent/dashboard";
	}

	// ---- Voyages (F05)

	@GetMapping("/trips")
	public String trips(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
		Company company = catalog.company(me.companyId());
		model.addAttribute("trips", trips.upcomingForCompany(me.companyId()));
		model.addAttribute("routes", catalog.routes().stream().filter(r -> r.getMode() == company.getMode()).toList());
		model.addAttribute("vehicles", catalog.vehicles(me.companyId()));
		return "agent/trips";
	}

	@PostMapping("/trips")
	public String scheduleTrip(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam UUID routeId,
			@RequestParam UUID vehicleId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureAt,
			@RequestParam long price, @RequestParam(defaultValue = "1") int days, RedirectAttributes flash) {
		try {
			int count = Math.max(1, Math.min(days, 30));
			for (int i = 0; i < count; i++) {
				catalog.scheduleTrip(me.companyId(), routeId, vehicleId, departureAt.plusDays(i), price);
			}
			flash.addFlashAttribute("success", count + " voyage(s) programmé(s).");
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/agent/trips";
	}

	@PostMapping("/trips/{tripId}/price")
	public String changePrice(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID tripId,
			@RequestParam long price, RedirectAttributes flash) {
		try {
			catalog.changePrice(me.companyId(), tripId, price);
			flash.addFlashAttribute("success", "Tarif modifié (tracé dans le journal d'audit).");
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/agent/trips";
	}

	@GetMapping("/trips/{tripId}/manifest")
	public String manifest(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID tripId, Model model) {
		model.addAttribute("trip", trips.view(tripId));
		model.addAttribute("tickets", bookings.manifest(me, tripId));
		return "agent/manifest";
	}

	// ---- Véhicules

	@GetMapping("/vehicles")
	public String vehicles(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
		model.addAttribute("vehicles", catalog.vehicles(me.companyId()));
		model.addAttribute("classes", ServiceClass.values());
		return "agent/vehicles";
	}

	@PostMapping("/vehicles")
	public String addVehicle(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam String registration,
			@RequestParam String model, @RequestParam ServiceClass serviceClass, @RequestParam int seatCount,
			@RequestParam int seatsPerRow, RedirectAttributes flash) {
		try {
			catalog.createVehicle(me.companyId(), registration, model, serviceClass, seatCount, seatsPerRow);
			flash.addFlashAttribute("success", "Véhicule ajouté.");
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/agent/vehicles";
	}

	// ---- Contrôle des billets (F15)

	@GetMapping("/scan")
	public String scanPage() {
		return "agent/scan";
	}

	@PostMapping("/scan")
	public String scan(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam String code, Model model) {
		model.addAttribute("result", tickets.validate(code, me));
		return "agent/scan";
	}

	// ---- Vente au guichet (F20)

	@GetMapping("/counter/{tripId}")
	public String counter(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID tripId, Model model) {
		TripView trip = trips.view(tripId);
		requireOwnTrip(me, trip);
		model.addAttribute("trip", trip);
		model.addAttribute("seatMap", trips.seatMap(tripId));
		model.addAttribute("maxPassengers", bookings.getMaxPassengers());
		model.addAttribute("lockMinutes", bookings.getLockMinutes());
		model.addAttribute("counter", true);
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new BookingForm());
		}
		return "booking/seats";
	}

	@PostMapping("/counter/{tripId}")
	public String sell(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID tripId,
			@ModelAttribute("form") BookingForm form, RedirectAttributes flash) {
		try {
			requireOwnTrip(me, trips.view(tripId));
			String name = form.getCustomerName() == null || form.getCustomerName().isBlank() ? "Client guichet"
					: form.getCustomerName().trim();
			Booking booking = bookings.create(new Customer(me.id(), name, me.email(), form.getCustomerPhone()), tripId,
					form.toInputs(), me.id());
			payments.initiate(me, booking.getId(), PaymentMethod.CASH, form.getCustomerPhone(), UUID.randomUUID().toString());
			flash.addFlashAttribute("success", "Vente enregistrée et encaissée en espèces. Imprimez les billets.");
			return "redirect:/bookings/" + booking.getId();
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
			return "redirect:/agent/counter/" + tripId;
		}
	}

	@GetMapping("/sales")
	public String sales(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
		model.addAttribute("bookings", bookings.forCompany(me.companyId()));
		return "agent/sales";
	}

	private static void requireOwnTrip(AppUserPrincipal me, TripView trip) {
		if (!trip.companyId().equals(me.companyId())) {
			throw new ForbiddenException("Ce voyage n'appartient pas à votre compagnie");
		}
	}
}
