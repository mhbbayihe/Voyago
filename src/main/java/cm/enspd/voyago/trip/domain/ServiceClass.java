package cm.enspd.voyago.trip.domain;

/** Niveau de confort ; son libellé dépend du mode de transport. */
public enum ServiceClass {
	STANDARD, PREMIUM;

	public String labelFor(TransportMode mode) {
		return switch (mode) {
			case BUS -> this == STANDARD ? "Classique" : "VIP";
			case TRAIN -> this == STANDARD ? "2e classe" : "1re classe";
			case PLANE -> this == STANDARD ? "Économique" : "Affaires";
		};
	}
}
