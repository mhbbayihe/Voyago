package cm.enspd.voyago.backoffice.web;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cm.enspd.voyago.auth.application.AuthService;
import cm.enspd.voyago.auth.domain.Role;
import cm.enspd.voyago.backoffice.application.StatsService;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.notification.application.NotificationService;
import cm.enspd.voyago.payment.application.PaymentService;
import cm.enspd.voyago.shared.audit.AuditService;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.trip.application.CatalogService;
import cm.enspd.voyago.trip.application.TripService;
import cm.enspd.voyago.trip.application.TripView;
import cm.enspd.voyago.trip.domain.TransportMode;

/** Administration de la plateforme (F04, F19, F21). */
@Controller
@RequestMapping("/admin")
public class AdminController {

	private final CatalogService catalog;
	private final TripService trips;
	private final AuthService users;
	private final BookingService bookings;
	private final PaymentService payments;
	private final NotificationService notifications;
	private final StatsService stats;
	private final AuditService audit;

	public AdminController(CatalogService catalog, TripService trips, AuthService users, BookingService bookings,
			PaymentService payments, NotificationService notifications, StatsService stats, AuditService audit) {
		this.catalog = catalog;
		this.trips = trips;
		this.users = users;
		this.bookings = bookings;
		this.payments = payments;
		this.notifications = notifications;
		this.stats = stats;
		this.audit = audit;
	}

	@GetMapping
	public String dashboard(Model model) {
		var week = trips.between(LocalDateTime.now(), LocalDateTime.now().plusDays(7));
		model.addAttribute("stats", stats.global());
		model.addAttribute("userCount", users.count());
		model.addAttribute("companyCount", catalog.companies().size());
		model.addAttribute("tripCount", week.size());
		model.addAttribute("avgFill", (int) week.stream().mapToInt(TripView::occupancyPercent).average().orElse(0));
		model.addAttribute("recent", bookings.all().stream().limit(8).toList());
		return "admin/dashboard";
	}

	// ---- Compagnies

	@GetMapping("/companies")
	public String companies(Model model) {
		model.addAttribute("companies", catalog.companies());
		return "admin/companies";
	}

	@PostMapping("/companies")
	public String createCompany(@RequestParam String name, @RequestParam TransportMode mode,
			@RequestParam(required = false) String phone, @RequestParam(required = false) String email,
			RedirectAttributes flash) {
		try {
			catalog.createCompany(name, mode, phone, email);
			flash.addFlashAttribute("success", "Compagnie créée.");
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/companies";
	}

	@PostMapping("/companies/{id}/toggle")
	public String toggleCompany(@PathVariable UUID id, @RequestParam boolean active) {
		catalog.setCompanyActive(id, active);
		return "redirect:/admin/companies";
	}

	// ---- Réseau : gares et lignes

	@GetMapping("/network")
	public String network(Model model) {
		model.addAttribute("stations", catalog.stations());
		model.addAttribute("routes", catalog.routes());
		return "admin/network";
	}

	@PostMapping("/stations")
	public String createStation(@RequestParam String name, @RequestParam String city, @RequestParam TransportMode mode,
			RedirectAttributes flash) {
		catalog.createStation(name, city, mode);
		flash.addFlashAttribute("success", "Point d'embarquement ajouté.");
		return "redirect:/admin/network";
	}

	@PostMapping("/routes")
	public String createRoute(@RequestParam UUID departureId, @RequestParam UUID arrivalId, @RequestParam int distanceKm,
			@RequestParam int durationMinutes, @RequestParam(defaultValue = "false") boolean bothWays,
			RedirectAttributes flash) {
		try {
			catalog.createRoute(departureId, arrivalId, distanceKm, durationMinutes);
			if (bothWays) {
				catalog.createRoute(arrivalId, departureId, distanceKm, durationMinutes);
			}
			flash.addFlashAttribute("success", "Ligne créée.");
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/network";
	}

	// ---- Utilisateurs

	@GetMapping("/users")
	public String users(Model model) {
		model.addAttribute("users", users.all());
		model.addAttribute("companies", catalog.companies());
		return "admin/users";
	}

	@PostMapping("/users")
	public String createUser(@RequestParam String fullName, @RequestParam String email,
			@RequestParam(required = false) String phone, @RequestParam String password, @RequestParam Role role,
			@RequestParam(required = false) UUID companyId, RedirectAttributes flash) {
		try {
			users.createStaff(fullName, email, phone, password, role, companyId);
			flash.addFlashAttribute("success", "Compte créé.");
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/users";
	}

	@PostMapping("/users/{id}/toggle")
	public String toggleUser(@PathVariable UUID id, @RequestParam boolean active) {
		users.setActive(id, active);
		return "redirect:/admin/users";
	}

	// ---- Suivi

	@GetMapping("/bookings")
	public String bookings(Model model) {
		model.addAttribute("bookings", bookings.all());
		return "admin/bookings";
	}

	@GetMapping("/payments")
	public String payments(Model model) {
		model.addAttribute("payments", payments.all());
		model.addAttribute("refunds", payments.allRefunds());
		return "admin/payments";
	}

	@GetMapping("/audit")
	public String audit(Model model) {
		model.addAttribute("logs", audit.latest());
		return "admin/audit";
	}

	@GetMapping("/notifications")
	public String notifications(Model model) {
		model.addAttribute("notifications", notifications.latest());
		return "admin/notifications";
	}
}
