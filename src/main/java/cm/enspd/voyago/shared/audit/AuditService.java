package cm.enspd.voyago.shared.audit;

import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

	private final AuditLogRepository repository;

	public AuditService(AuditLogRepository repository) {
		this.repository = repository;
	}

	public void record(String action, String details) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean known = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
		repository.save(new AuditLog(known ? auth.getName() : "système", action, details));
	}

	public List<AuditLog> latest() {
		return repository.findTop200ByOrderByCreatedAtDesc();
	}
}
