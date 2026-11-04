package cm.enspd.voyago.booking.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Pattern State : chaque état connaît les transitions autorisées.
 * PENDING_PAYMENT → CONFIRMED | EXPIRED | CANCELLED ; CONFIRMED → CANCELLED ; EXPIRED et CANCELLED sont finaux.
 */
public enum BookingStatus {

	PENDING_PAYMENT("En attente de paiement", "warning") {
		@Override
		public Set<BookingStatus> next() {
			return EnumSet.of(CONFIRMED, EXPIRED, CANCELLED);
		}
	},
	CONFIRMED("Confirmée", "success") {
		@Override
		public Set<BookingStatus> next() {
			return EnumSet.of(CANCELLED);
		}
	},
	EXPIRED("Expirée", "muted") {
		@Override
		public Set<BookingStatus> next() {
			return EnumSet.noneOf(BookingStatus.class);
		}
	},
	CANCELLED("Annulée", "danger") {
		@Override
		public Set<BookingStatus> next() {
			return EnumSet.noneOf(BookingStatus.class);
		}
	};

	private final String label;
	private final String tone;

	BookingStatus(String label, String tone) {
		this.label = label;
		this.tone = tone;
	}

	public abstract Set<BookingStatus> next();

	public boolean canTransitionTo(BookingStatus target) {
		return next().contains(target);
	}

	public String getLabel() {
		return label;
	}

	public String getTone() {
		return tone;
	}
}
