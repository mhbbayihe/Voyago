package cm.enspd.voyago.booking.domain;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * RG5 : barème d'annulation paramétrable.
 * Par défaut : plus de 24 h avant le départ → 80 % ; entre 24 h et 2 h → 50 % ; moins de 2 h → 0 %.
 */
public class RefundPolicy {

	private final int fullWindowHours;
	private final int fullWindowPercent;
	private final int partialWindowHours;
	private final int partialWindowPercent;

	public RefundPolicy(int fullWindowHours, int fullWindowPercent, int partialWindowHours, int partialWindowPercent) {
		this.fullWindowHours = fullWindowHours;
		this.fullWindowPercent = fullWindowPercent;
		this.partialWindowHours = partialWindowHours;
		this.partialWindowPercent = partialWindowPercent;
	}

	public RefundQuote quote(long paidAmount, LocalDateTime departureAt, LocalDateTime now) {
		long minutesBefore = Duration.between(now, departureAt).toMinutes();
		int percent;
		if (minutesBefore > fullWindowHours * 60L) {
			percent = fullWindowPercent;
		} else if (minutesBefore >= partialWindowHours * 60L) {
			percent = partialWindowPercent;
		} else {
			percent = 0;
		}
		return new RefundQuote(percent, paidAmount * percent / 100);
	}

	public String describe() {
		return "Plus de " + fullWindowHours + " h avant le départ : " + fullWindowPercent + " % remboursés · entre "
				+ fullWindowHours + " h et " + partialWindowHours + " h : " + partialWindowPercent + " % · moins de "
				+ partialWindowHours + " h : aucun remboursement";
	}

	public record RefundQuote(int percent, long amount) {
	}
}
