package cm.enspd.voyago.payment.infrastructure;

import java.util.List;
import java.util.UUID;

import cm.enspd.voyago.payment.domain.port.PaymentProvider;

/**
 * Simulateur d'opérateur Mobile Money (sandbox). Il vérifie que le numéro appartient bien à
 * l'opérateur, puis renvoie une référence externe ; la confirmation arrive ensuite par webhook,
 * comme avec les vraies API MTN MoMo et Orange Money.
 */
abstract class SimulatedMobileMoneyProvider implements PaymentProvider {

	protected abstract String operatorName();

	protected abstract String referencePrefix();

	/** Préfixes des numéros camerounais (9 chiffres) de l'opérateur. */
	protected abstract List<String> prefixes();

	@Override
	public ProviderResult initiate(String payerPhone, long amount, String reference) {
		String phone = normalize(payerPhone);
		if (phone == null) {
			return ProviderResult.rejected("Numéro invalide : saisissez 9 chiffres, par exemple 6XX XX XX XX");
		}
		if (prefixes().stream().noneMatch(phone::startsWith)) {
			return ProviderResult.rejected("Ce numéro n'est pas un numéro " + operatorName());
		}
		String externalRef = referencePrefix() + "-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
		return ProviderResult.pending(externalRef,
				"Demande envoyée au " + phone + " : composez votre code " + operatorName() + " pour valider.");
	}

	@Override
	public ProviderResult refund(String externalReference, long amount) {
		return ProviderResult.confirmed(referencePrefix() + "-RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
	}

	static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String digits = raw.replaceAll("[^0-9]", "");
		if (digits.startsWith("237") && digits.length() == 12) {
			digits = digits.substring(3);
		}
		return digits.matches("6\\d{8}") ? digits : null;
	}
}
