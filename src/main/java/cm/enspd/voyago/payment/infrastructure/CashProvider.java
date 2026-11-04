package cm.enspd.voyago.payment.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Component;

import cm.enspd.voyago.payment.domain.PaymentMethod;
import cm.enspd.voyago.payment.domain.port.PaymentProvider;

/** Encaissement en espèces au guichet (F20) : confirmé immédiatement par l'agent. */
@Component
class CashProvider implements PaymentProvider {

	@Override
	public PaymentMethod method() {
		return PaymentMethod.CASH;
	}

	@Override
	public ProviderResult initiate(String payerPhone, long amount, String reference) {
		return ProviderResult.confirmed("CASH-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase());
	}

	@Override
	public ProviderResult refund(String externalReference, long amount) {
		return ProviderResult.confirmed("CASH-RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
	}
}
