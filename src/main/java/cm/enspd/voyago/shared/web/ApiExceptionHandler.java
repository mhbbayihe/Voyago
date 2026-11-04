package cm.enspd.voyago.shared.web;

import java.net.URI;
import java.util.stream.Collectors;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cm.enspd.voyago.booking.domain.SeatUnavailableException;
import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.shared.domain.ForbiddenException;
import cm.enspd.voyago.shared.domain.NotFoundException;

/** Format d'erreur unifié de l'API (RFC 7807 « application/problem+json »). */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException e) {
		return problem(HttpStatus.NOT_FOUND, e.getCode(), e.getMessage());
	}

	@ExceptionHandler({ ForbiddenException.class, AccessDeniedException.class })
	ProblemDetail forbidden(RuntimeException e) {
		return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage());
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail unauthorized(AuthenticationException e) {
		return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Identifiants invalides");
	}

	@ExceptionHandler({ SeatUnavailableException.class, OptimisticLockingFailureException.class })
	ProblemDetail conflict(RuntimeException e) {
		String code = e instanceof BusinessException b ? b.getCode() : "CONFLICT";
		return problem(HttpStatus.CONFLICT, code, e.getMessage());
	}

	@ExceptionHandler(BusinessException.class)
	ProblemDetail business(BusinessException e) {
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, e.getCode(), e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalid(MethodArgumentNotValidException e) {
		String detail = e.getBindingResult().getFieldErrors().stream()
				.map(f -> f.getField() + " : " + f.getDefaultMessage()).collect(Collectors.joining(" ; "));
		return problem(HttpStatus.BAD_REQUEST, "VALIDATION", detail);
	}

	private static ProblemDetail problem(HttpStatus status, String code, String detail) {
		ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
		p.setType(URI.create("https://voyago.cm/errors/" + code.toLowerCase().replace('_', '-')));
		p.setTitle(status.getReasonPhrase());
		p.setProperty("code", code);
		return p;
	}
}
