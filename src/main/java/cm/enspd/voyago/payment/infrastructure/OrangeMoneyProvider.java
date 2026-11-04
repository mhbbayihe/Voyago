package cm.enspd.voyago.payment.infrastructure;

import java.util.List;

import org.springframework.stereotype.Component;

import cm.enspd.voyago.payment.domain.PaymentMethod;

@Component
class OrangeMoneyProvider extends SimulatedMobileMoneyProvider {

	@Override
	public PaymentMethod method() {
		return PaymentMethod.ORANGE_MONEY;
	}

	@Override
	protected String operatorName() {
		return "Orange Money";
	}

	@Override
	protected String referencePrefix() {
		return "OM";
	}

	@Override
	protected List<String> prefixes() {
		return List.of("69", "655", "656", "657", "658", "659", "685", "686", "687", "688", "689");
	}
}
