package cm.enspd.voyago.payment.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.payment.application.PaymentService;
import cm.enspd.voyago.payment.domain.Payment;
import cm.enspd.voyago.payment.domain.PaymentMethod;
import cm.enspd.voyago.payment.infrastructure.WebhookSignature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@Tag(name = "Paiements")
public class PaymentApiController {

	private final PaymentService payments;
	private final WebhookSignature signature;

	public PaymentApiController(PaymentService payments, WebhookSignature signature) {
		this.payments = payments;
		this.signature = signature;
	}

	public record InitiatePaymentRequest(@NotNull UUID bookingId, @NotNull PaymentMethod method, @NotBlank String phone) {
	}

	public record PaymentResponse(UUID id, UUID bookingId, String bookingReference, long amount, String method,
			String status, String externalReference, String failureReason) {
		static PaymentResponse of(Payment p) {
			return new PaymentResponse(p.getId(), p.getBookingId(), p.getBookingReference(), p.getAmount(),
					p.getMethod().name(), p.getStatus().name(), p.getExternalReference(), p.getFailureReason());
		}
	}

	@PostMapping("/api/v1/payments")
	@Operation(summary = "Initier un paiement Mobile Money (en-tête Idempotency-Key recommandé)")
	public ResponseEntity<PaymentResponse> initiate(@AuthenticationPrincipal AppUserPrincipal me,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody InitiatePaymentRequest req) {
		Payment p = payments.initiate(me, req.bookingId(), req.method(), req.phone(), idempotencyKey);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(PaymentResponse.of(p));
	}

	@GetMapping("/api/v1/payments/{id}")
	@Operation(summary = "Suivre le statut d'un paiement")
	public PaymentResponse get(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID id) {
		return PaymentResponse.of(payments.getOwned(me, id));
	}

	public record WebhookRequest(@NotBlank String externalReference, @NotBlank String status, String reason) {
	}

	/**
	 * Notification du fournisseur. Signature obligatoire :
	 * en-tête X-VoyaGo-Signature = HMAC-SHA256(secret, "externalReference|status") en hexadécimal.
	 * status = SUCCESSFUL ou FAILED.
	 */
	@PostMapping("/api/v1/payments/webhook/{provider}")
	@Operation(summary = "Webhook du fournisseur de paiement (signé HMAC)")
	public ResponseEntity<PaymentResponse> webhook(@PathVariable String provider,
			@RequestHeader(value = "X-VoyaGo-Signature", required = false) String sig,
			@Valid @RequestBody WebhookRequest req) {
		if (!signature.isValid(req.externalReference(), req.status(), sig)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		PaymentMethod method = switch (provider.toLowerCase()) {
			case "mtn", "mtn_momo", "momo" -> PaymentMethod.MTN_MOMO;
			case "orange", "orange_money", "om" -> PaymentMethod.ORANGE_MONEY;
			default -> null;
		};
		if (method == null) {
			return ResponseEntity.notFound().build();
		}
		Payment p = payments.handleProviderResult(method, req.externalReference(),
				"SUCCESSFUL".equalsIgnoreCase(req.status()), req.reason());
		return ResponseEntity.ok(PaymentResponse.of(p));
	}
}
