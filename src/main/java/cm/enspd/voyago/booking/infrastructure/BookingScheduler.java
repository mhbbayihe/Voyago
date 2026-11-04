package cm.enspd.voyago.booking.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cm.enspd.voyago.booking.application.BookingService;

/** Tâches planifiées : expiration des verrous de sièges (RG2) et rappels de départ (F16). */
@Component
class BookingScheduler {

	private static final Logger log = LoggerFactory.getLogger(BookingScheduler.class);

	private final BookingService bookings;

	BookingScheduler(BookingService bookings) {
		this.bookings = bookings;
	}

	@Scheduled(fixedDelayString = "${voyago.booking.expiration-check-ms}", initialDelay = 10000)
	void expireUnpaidBookings() {
		int expired = bookings.expireOverdue();
		if (expired > 0) {
			log.info("{} réservation(s) non payée(s) expirée(s), sièges libérés", expired);
		}
	}

	@Scheduled(fixedDelay = 600000, initialDelay = 20000)
	void sendDepartureReminders() {
		bookings.sendDepartureReminders();
	}
}
