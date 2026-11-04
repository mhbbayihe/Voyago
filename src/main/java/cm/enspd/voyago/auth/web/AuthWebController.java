package cm.enspd.voyago.auth.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.auth.application.AuthService;
import cm.enspd.voyago.shared.domain.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Controller
public class AuthWebController {

	private final AuthService auth;

	public AuthWebController(AuthService auth) {
		this.auth = auth;
	}

	public static class RegisterForm {
		@NotBlank(message = "Nom obligatoire")
		private String fullName;
		@NotBlank(message = "E-mail obligatoire")
		@Email(message = "E-mail invalide")
		private String email;
		private String phone;
		@Size(min = 8, message = "8 caractères minimum")
		private String password;
		private boolean consent;

		public String getFullName() { return fullName; }
		public void setFullName(String fullName) { this.fullName = fullName; }
		public String getEmail() { return email; }
		public void setEmail(String email) { this.email = email; }
		public String getPhone() { return phone; }
		public void setPhone(String phone) { this.phone = phone; }
		public String getPassword() { return password; }
		public void setPassword(String password) { this.password = password; }
		public boolean isConsent() { return consent; }
		public void setConsent(boolean consent) { this.consent = consent; }
	}

	@GetMapping("/login")
	public String login() {
		return "auth/login";
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("form", new RegisterForm());
		return "auth/register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("form") RegisterForm form,
			org.springframework.validation.BindingResult errors, Model model, RedirectAttributes flash) {
		if (!form.isConsent()) {
			errors.rejectValue("consent", "consent", "Vous devez accepter la politique de confidentialité");
		}
		if (errors.hasErrors()) {
			return "auth/register";
		}
		try {
			auth.registerClient(form.getFullName(), form.getEmail(), form.getPhone(), form.getPassword());
		} catch (BusinessException e) {
			model.addAttribute("error", e.getMessage());
			return "auth/register";
		}
		flash.addFlashAttribute("success", "Compte créé ! Connectez-vous pour réserver.");
		return "redirect:/login";
	}

	/** Redirection après connexion selon le rôle. */
	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal AppUserPrincipal me) {
		return switch (me.role()) {
			case ADMIN -> "redirect:/admin";
			case AGENT -> "redirect:/agent";
			case CLIENT -> "redirect:/account/bookings";
		};
	}
}
