package cm.enspd.voyago.booking.domain;

import java.util.Collection;

import cm.enspd.voyago.shared.domain.BusinessException;

/** Levée quand un siège demandé est déjà verrouillé ou vendu (RG1). */
public class SeatUnavailableException extends BusinessException {

	public SeatUnavailableException(Collection<Integer> seats) {
		super("SEAT_UNAVAILABLE", seats.isEmpty() ? "Un des sièges choisis vient d'être pris par un autre voyageur"
				: "Siège(s) " + seats + " déjà pris par un autre voyageur. Merci d'en choisir d'autres.");
	}
}
