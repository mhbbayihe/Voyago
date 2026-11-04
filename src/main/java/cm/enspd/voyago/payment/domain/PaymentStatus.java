package cm.enspd.voyago.payment.domain;

public enum PaymentStatus {
	PENDING("En attente", "warning"),
	SUCCEEDED("Réussi", "success"),
	FAILED("Échoué", "danger"),
	CANCELLED("Annulé", "muted"),
	PARTIALLY_REFUNDED("Remboursé partiellement", "info"),
	REFUNDED("Remboursé", "info");

	private final String label;
	private final String tone;

	PaymentStatus(String label, String tone) {
		this.label = label;
		this.tone = tone;
	}

	public String getLabel() { return label; }
	public String getTone() { return tone; }
}
