package cm.enspd.voyago.trip.application;

import java.time.LocalDate;
import java.util.UUID;

import cm.enspd.voyago.trip.domain.ServiceClass;
import cm.enspd.voyago.trip.domain.TransportMode;

/**
 * Critères de recherche (F06). Seuls from, to et date sont obligatoires ; les autres sont des filtres.
 * period : MORNING (avant 12 h), AFTERNOON (12 h - 18 h), EVENING (après 18 h).
 * sort : DEPARTURE (défaut), PRICE, DURATION.
 */
public record TripSearchCriteria(String from, String to, LocalDate date, int passengers, TransportMode mode,
		UUID companyId, Long maxPrice, String period, ServiceClass serviceClass, String sort) {
}
