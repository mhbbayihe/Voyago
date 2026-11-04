package cm.enspd.voyago.notification.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.notification.application.NotificationService;

/** Boîte de réception de démonstration : e-mails et SMS envoyés à l'utilisateur. */
@Controller
public class InboxController {

	private final NotificationService notifications;

	public InboxController(NotificationService notifications) {
		this.notifications = notifications;
	}

	@GetMapping("/account/inbox")
	public String inbox(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
		model.addAttribute("notifications", notifications.forUser(me.id()));
		return "account/inbox";
	}
}
