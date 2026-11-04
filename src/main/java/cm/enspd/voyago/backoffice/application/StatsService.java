package cm.enspd.voyago.backoffice.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.booking.domain.BookingStatus;
import cm.enspd.voyago.trip.domain.TransportMode;

/** F19 : tableaux de bord (ventes, revenus par ligne et par période, répartition par mode). */
@Service
public class StatsService {

	private final BookingService bookings;

	public StatsService(BookingService bookings) {
		this.bookings = bookings;
	}

	public record LabelValue(String label, long tickets, long revenue) {
	}

	public record DayValue(LocalDate day, long revenue, int percentOfMax) {
	}

	public record SalesStats(long ticketsSold, long revenue, long confirmed, long pending, long cancelled, long expired,
			long refunded, List<LabelValue> byRoute, List<DayValue> last14Days, Map<TransportMode, Long> revenueByMode) {

		public long averageBasket() {
			return confirmed == 0 ? 0 : revenue / Math.max(1, confirmed);
		}
	}

	public SalesStats forCompany(UUID companyId) {
		return compute(bookings.forCompany(companyId));
	}

	public SalesStats global() {
		return compute(bookings.all());
	}

	private SalesStats compute(List<Booking> list) {
		long tickets = 0, revenue = 0, confirmed = 0, pending = 0, cancelled = 0, expired = 0, refunded = 0;
		Map<String, long[]> byRoute = new LinkedHashMap<>();
		Map<TransportMode, Long> byMode = new EnumMap<>(TransportMode.class);
		LocalDate today = LocalDate.now();
		Map<LocalDate, Long> byDay = new LinkedHashMap<>();
		for (int i = 13; i >= 0; i--) {
			byDay.put(today.minusDays(i), 0L);
		}

		for (Booking b : list) {
			switch (b.getStatus()) {
				case CONFIRMED -> {
					confirmed++;
					tickets += b.getPassengers().size();
				}
				case PENDING_PAYMENT -> pending++;
				case CANCELLED -> cancelled++;
				case EXPIRED -> expired++;
			}
			if (b.getStatus() == BookingStatus.CANCELLED) {
				refunded += b.getRefundAmount();
			}
			long retained = b.getRetainedAmount();
			if (retained == 0) {
				continue;
			}
			revenue += retained;
			long[] route = byRoute.computeIfAbsent(b.getTripLabel(), k -> new long[2]);
			route[0] += b.getStatus() == BookingStatus.CONFIRMED ? b.getPassengers().size() : 0;
			route[1] += retained;
			byMode.merge(b.getMode(), retained, Long::sum);
			LocalDate day = b.getConfirmedAt().toLocalDate();
			byDay.computeIfPresent(day, (d, v) -> v + retained);
		}

		List<LabelValue> routes = new ArrayList<>();
		byRoute.forEach((label, v) -> routes.add(new LabelValue(label, v[0], v[1])));
		routes.sort(Comparator.comparingLong(LabelValue::revenue).reversed());

		long max = byDay.values().stream().mapToLong(Long::longValue).max().orElse(0);
		List<DayValue> days = new ArrayList<>();
		byDay.forEach((d, v) -> days.add(new DayValue(d, v, max == 0 ? 0 : (int) Math.round(100.0 * v / max))));

		return new SalesStats(tickets, revenue, confirmed, pending, cancelled, expired, refunded,
				routes.subList(0, Math.min(8, routes.size())), days, byMode);
	}
}
