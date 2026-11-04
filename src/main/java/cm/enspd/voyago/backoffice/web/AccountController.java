package cm.enspd.voyago.backoffice.web;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.auth.application.AuthService;
import cm.enspd.voyago.auth.domain.User;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.booking.domain.BookingStatus;
import cm.enspd.voyago.notification.application.NotificationService;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.trip.application.CatalogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Espace « Mon compte » : profil, sécurité, résumé de l'activité de voyage. */
@Controller
public class AccountController {

	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
	private final AuthService auth;
	private final BookingService bookings;
	private final NotificationService notifications;
	private final CatalogService catalog;

	public AccountController(AuthService auth, BookingService bookings, NotificationService notifications,
			CatalogService catalog) {
		this.auth = auth;
		this.bookings = bookings;
		this.notifications = notifications;
		this.catalog = catalog;
	}

	/** Résumé de l'activité du voyageur. */
	public record TravelSummary(int totalBookings, int upcomingTrips, int completedTrips, int ticketCount,
			long totalSpent, long totalRefunded, String favoriteRoute, Booking nextTrip, long daysToNextTrip) {
	}

	@GetMapping("/account/profile")
	public String profile(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
		User user = auth.get(me.id());
		List<Booking> list = bookings.forCustomer(me.id());
		model.addAttribute("user", user);
		model.addAttribute("summary", summarize(list));
		model.addAttribute("recentBookings", list.stream()
				.sorted(Comparator.comparing(Booking::getCreatedAt).reversed()).limit(5).toList());
		model.addAttribute("recentMessages", notifications.forUser(me.id()).stream().limit(4).toList());
		model.addAttribute("memberDays", Math.max(1, ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now()) + 1));
		if (user.getCompanyId() != null) {
			model.addAttribute("company", catalog.company(user.getCompanyId()));
		}
		return "account/profile";
	}

	private TravelSummary summarize(List<Booking> list) {
		LocalDateTime now = LocalDateTime.now();
		List<Booking> confirmed = list.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).toList();
		List<Booking> upcoming = confirmed.stream().filter(b -> b.getDepartureAt().isAfter(now))
				.sorted(Comparator.comparing(Booking::getDepartureAt)).toList();
		int completed = (int) confirmed.stream().filter(b -> !b.getDepartureAt().isAfter(now)).count();
		int tickets = confirmed.stream().mapToInt(b -> b.getPassengers().size()).sum();
		long spent = list.stream().mapToLong(Booking::getRetainedAmount).sum();
		long refunded = list.stream().mapToLong(Booking::getRefundAmount).sum();
		String favorite = list.stream().filter(b -> b.getConfirmedAt() != null)
				.collect(Collectors.groupingBy(Booking::getTripLabel, Collectors.counting())).entrySet().stream()
				.max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
		Optional<Booking> next = upcoming.stream().findFirst();
		long days = next.map(b -> ChronoUnit.DAYS.between(now.toLocalDate(), b.getDepartureAt().toLocalDate())).orElse(0L);
		return new TravelSummary(list.size(), upcoming.size(), completed, tickets, spent, refunded, favorite,
				next.orElse(null), days);
	}

	@PostMapping("/account/profile")
	public String updateProfile(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam String fullName,
			@RequestParam(required = false) String phone, RedirectAttributes flash, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			User user = auth.updateProfile(me.id(), fullName, phone);
			refreshSession(user, request, response);
			flash.addFlashAttribute("success", "Vos informations ont été mises à jour.");
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/account/profile";
	}

	@PostMapping("/account/password")
	public String changePassword(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam String currentPassword,
			@RequestParam String newPassword, @RequestParam String confirmation, RedirectAttributes flash,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			auth.changePassword(me.id(), currentPassword, newPassword, confirmation);
			refreshSession(auth.get(me.id()), request, response);
			flash.addFlashAttribute("success", "Mot de passe modifié.");
		} catch (BusinessException e) {
			flash.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/account/profile#securite";
	}

	/** Met à jour l'utilisateur en session pour que le nouveau nom s'affiche immédiatement. */
	private void refreshSession(User user, HttpServletRequest request, HttpServletResponse response) {
		AppUserPrincipal principal = AppUserPrincipal.of(user);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}
}
