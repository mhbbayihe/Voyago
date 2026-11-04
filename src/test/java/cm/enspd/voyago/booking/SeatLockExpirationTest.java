package cm.enspd.voyago.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cm.enspd.voyago.TestFixtures;
import cm.enspd.voyago.auth.application.AuthService;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.application.BookingService.Customer;
import cm.enspd.voyago.booking.application.BookingService.PassengerInput;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.booking.domain.BookingStatus;
import cm.enspd.voyago.trip.application.CatalogService;
import cm.enspd.voyago.trip.application.TripService;

/** RG2 : sans paiement, le verrou expire et les sièges sont libérés (délai ramené à 0 min pour le test). */
@SpringBootTest(properties = { "voyago.booking.seat-lock-minutes=0",
		"spring.datasource.url=jdbc:h2:mem:voyago-expiry;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1" })
class SeatLockExpirationTest {

	@Autowired CatalogService catalog;
	@Autowired AuthService auth;
	@Autowired TripService trips;
	@Autowired BookingService bookings;

	@Test
	void verrouExpire_siegesLiberes_etReservationExpiree() throws InterruptedException {
		var f = TestFixtures.create(catalog, auth, LocalDateTime.now().plusDays(2));
		Booking booking = bookings.create(Customer.of(f.client()), f.trip().getId(),
				List.of(new PassengerInput("Jean", "Kamga", "CNI1", 12)), null);
		assertThat(trips.seatMap(f.trip().getId()).occupied()).contains(12);

		Thread.sleep(50);
		int expired = bookings.expireOverdue();

		assertThat(expired).isGreaterThanOrEqualTo(1);
		assertThat(bookings.get(booking.getId()).getStatus()).isEqualTo(BookingStatus.EXPIRED);
		assertThat(trips.seatMap(f.trip().getId()).occupied()).doesNotContain(12);
	}
}
