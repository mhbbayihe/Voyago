package cm.enspd.voyago.auth.domain;

public enum Role {
	CLIENT("Client"), AGENT("Agent de compagnie"), ADMIN("Administrateur");

	private final String label;

	Role(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
