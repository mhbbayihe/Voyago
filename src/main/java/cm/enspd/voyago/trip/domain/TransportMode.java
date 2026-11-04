package cm.enspd.voyago.trip.domain;

/** Modes de transport proposés par la plateforme. */
public enum TransportMode {
	BUS("Bus", "Gare routière", "bus"),
	TRAIN("Train", "Gare ferroviaire", "train-front"),
	PLANE("Avion", "Aéroport", "plane");

	private final String label;
	private final String stationLabel;
	/** Nom de l'icône Lucide (sprite img/icons.svg). */
	private final String icon;

	TransportMode(String label, String stationLabel, String icon) {
		this.label = label;
		this.stationLabel = stationLabel;
		this.icon = icon;
	}

	public String getLabel() { return label; }
	public String getStationLabel() { return stationLabel; }
	public String getIcon() { return icon; }
}
