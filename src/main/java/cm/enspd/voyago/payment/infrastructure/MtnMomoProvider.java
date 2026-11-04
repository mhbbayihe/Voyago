package cm.enspd.voyago.payment.infrastructure;

import java.util.List;

import org.springframework.stereotype.Component;

import cm.enspd.voyago.payment.domain.PaymentMethod;

@Component
class MtnMomoProvider extends SimulatedMobileMoneyProvider {

	@Override
	public PaymentMethod method() {
		return PaymentMethod.MTN_MOMO;
	}

	@Override
	protected String operatorName() {
		return "MTN MoMo";
	}

	@Override
	protected String referencePrefix() {
		return "MOMO";
	}

	@Override
	protected List<String> prefixes() {
		return List.of("67", "650", "651", "652", "653", "654", "680", "681", "682", "683", "684");
	}
}
