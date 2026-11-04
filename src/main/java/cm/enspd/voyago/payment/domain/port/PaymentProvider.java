package cm.enspd.voyago.payment.domain.port;

import cm.enspd.voyago.payment.domain.PaymentMethod;

/**
 * Pattern Strategy / Adapter : chaque fournisseur (MTN MoMo, Orange Money, espèces…) implémente
 * cette interface. Ajouter un fournisseur réel (API sandbox, CinetPay, Campay…) consiste à écrire
 * un nouvel adaptateur, sans toucher au reste de l'application.
 */
public interface PaymentProvider {

	PaymentMethod method();

	/** Demande de paiement envoyée au fournisseur (ex. notification USSD sur le téléphone du client). */
	ProviderResult initiate(String payerPhone, long amount, String reference);

	ProviderResult refund(String externalReference, long amount);

	/**
	 * @param accepted  la demande a été acceptée par le fournisseur
	 * @param immediate le paiement est confirmé immédiatement (espèces), sans attendre de webhook
	 */
	record ProviderResult(boolean accepted, boolean immediate, String externalReference, String message) {

		public static ProviderResult pending(String externalReference, String message) {
			return new ProviderResult(true, false, externalReference, message);
		}

		public static ProviderResult confirmed(String externalReference) {
			return new ProviderResult(true, true, externalReference, "Paiement confirmé");
		}

		public static ProviderResult rejected(String message) {
			return new ProviderResult(false, false, null, message);
		}
	}
}
