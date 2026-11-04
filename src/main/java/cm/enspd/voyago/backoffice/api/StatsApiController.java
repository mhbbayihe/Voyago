package cm.enspd.voyago.backoffice.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.backoffice.application.StatsService;
import cm.enspd.voyago.backoffice.application.StatsService.SalesStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Statistiques")
public class StatsApiController {

	private final StatsService stats;

	public StatsApiController(StatsService stats) {
		this.stats = stats;
	}

	@GetMapping("/api/v1/admin/stats/sales")
	@Operation(summary = "Statistiques de ventes (agent : sa compagnie ; admin : toute la plateforme)")
	public SalesStats sales(@AuthenticationPrincipal AppUserPrincipal me) {
		return me.isAdmin() ? stats.global() : stats.forCompany(me.companyId());
	}
}
