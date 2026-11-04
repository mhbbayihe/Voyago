package cm.enspd.voyago.booking.domain;

public enum TicketStatus {
	VALID("Valide", "success"), USED("Utilisé", "muted"), CANCELLED("Annulé", "danger");

	private final String label;
	private final String tone;

	TicketStatus(String label, String tone) {
		this.label = label;
		this.tone = tone;
	}

	public String getLabel() { return label; }
	public String getTone() { return tone; }
}
