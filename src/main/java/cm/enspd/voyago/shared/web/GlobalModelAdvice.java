package cm.enspd.voyago.shared.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.ForbiddenException;
import cm.enspd.voyago.shared.domain.NotFoundException;
import cm.enspd.voyago.trip.domain.TransportMode;

/** Données communes à toutes les pages et gestion des erreurs métier côté site web. */
@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {

	@ModelAttribute("me")
	public AppUserPrincipal currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.getPrincipal() instanceof AppUserPrincipal p ? p : null;
	}

	@ModelAttribute("modes")
	public TransportMode[] modes() {
		return TransportMode.values();
	}

	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String notFound(NotFoundException e, Model model) {
		model.addAttribute("title", "Page introuvable");
		model.addAttribute("message", e.getMessage());
		return "error";
	}

	@ExceptionHandler(ForbiddenException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public String forbidden(ForbiddenException e, Model model) {
		model.addAttribute("title", "Accès refusé");
		model.addAttribute("message", e.getMessage());
		return "error";
	}

	@ExceptionHandler(BusinessException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public String business(BusinessException e, Model model) {
		model.addAttribute("title", "Opération impossible");
		model.addAttribute("message", e.getMessage());
		return "error";
	}
}
