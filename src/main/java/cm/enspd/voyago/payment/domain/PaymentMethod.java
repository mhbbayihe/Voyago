package cm.enspd.voyago.payment.domain;

public enum PaymentMethod {
	MTN_MOMO("MTN Mobile Money"), ORANGE_MONEY("Orange Money"), CASH("Espèces (guichet)");

	private final String label;

	PaymentMethod(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
