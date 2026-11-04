package cm.enspd.voyago.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cm.enspd.voyago.TestFixtures;
import cm.enspd.voyago.auth.application.AuthService;
import cm.enspd.voyago.booking.application.BookingService;
import cm.enspd.voyago.booking.application.BookingService.Customer;
import cm.enspd.voyago.booking.application.BookingService.PassengerInput;
import cm.enspd.voyago.booking.application.TicketService;
import cm.enspd.voyago.booking.domain.Booking;
import cm.enspd.voyago.booking.domain.BookingStatus;
import cm.enspd.voyago.booking.domain.SeatUnavailableException;
import cm.enspd.voyago.notification.application.NotificationService;
import cm.enspd.voyago.payment.application.PaymentService;
import cm.enspd.voyago.payment.domain.Payment;
import cm.enspd.voyago.payment.domain.PaymentMethod;
import cm.enspd.voyago.payment.domain.PaymentStatus;
import cm.enspd.voyago.trip.application.CatalogService;
import cm.enspd.voyago.trip.application.TripService;

/** Tests d'intégration du parcours complet et des règles critiques (RG1, RG4, RG5, RG6). */
@SpringBootTest
class BookingFlowIntegrationTest {

	@Autowired CatalogService catalog;
	@Autowired AuthService auth;
	@Autowired TripService trips;
	@Autowired BookingService bookings;
	@Autowired PaymentService payments;
	@Autowired TicketService tickets;
	@Autowired NotificationService notifications;

	private static List<PassengerInput> passengers(int... seats) {
		List<PassengerInput> list = new ArrayList<>();
		for (int s : seats) {
			list.add(new PassengerInput("Prénom" + s, "Nom" + s, "CNI" + s, s));
		}
		return list;
	}

	private Booking bookAndPay(TestFixtures.Fixture f, int... seats) {
		Booking booking = bookings.create(Customer.of(f.client()), f.trip().getId(), passengers(seats), null);
		Payment p = payments.initiate(f.client(), booking.getId(), PaymentMethod.MTN_MOMO, "677112233", UUID.randomUUID().toString());
		payments.handleProviderResult(PaymentMethod.MTN_MOMO, p.getExternalReference(), true, null);
		return bookings.get(booking.getId());
	}

	@Test
	void parcoursComplet_reservation_paiement_billets_controle_notification() {
		var f = TestFixtures.create(catalog, auth, LocalDateTime.now().plusDays(3));

		Booking booking = bookAndPay(f, 3, 4);

		assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
		assertThat(booking.getTotalAmount()).isEqualTo(8000);
		var issued = bookings.tickets(booking.getId());
		assertThat(issued).hasSize(2);
		assertThat(trips.view(f.trip().getId()).availableSeats()).isEqualTo(38);
		assertThat(notifications.forUser(f.client().id())).anyMatch(n -> n.getSubject() != null
				&& n.getSubject().contains(booking.getReference()) && n.getAttachmentName() != null);
		assertThat(tickets.pdf(issued)).startsWith("%PDF".getBytes());

		// RG6 : premier scan accepté, second refusé
		String qr = tickets.qrPayload(issued.get(0));
		assertThat(tickets.validate(qr, f.agent()).valid()).isTrue();
		var second = tickets.validate(qr, f.agent());
		assertThat(second.valid()).isFalse();
		assertThat(second.message()).contains("déjà utilisé");
	}

	@Test
	void unSiegeDejaVerrouilleEstRefuse() {
		var f = TestFixtures.create(catalog, auth, LocalDateTime.now().plusDays(3));
		bookings.create(Customer.of(f.client()), f.trip().getId(), passengers(10), null);

		assertThatThrownBy(() -> bookings.create(Customer.of(f.client()), f.trip().getId(), passengers(10, 11), null))
				.isInstanceOf(SeatUnavailableException.class)
				.hasMessageContaining("10");
	}

	@Test
	void concurrence_unSeulGagnantPourLeMemeSiege() throws Exception {
		var f = TestFixtures.create(catalog, auth, LocalDateTime.now().plusDays(3));
		int threads = 12;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			results.add(pool.submit(() -> {
				start.await();
				try {
					bookings.create(Customer.of(f.client()), f.trip().getId(), passengers(7), null);
					return true;
				} catch (SeatUnavailableException e) {
					return false;
				}
			}));
		}
		start.countDown();
		int winners = 0;
		for (Future<Boolean> r : results) {
			if (r.get(30, TimeUnit.SECONDS)) {
				winners++;
			}
		}
		pool.shutdown();

		assertThat(winners).isEqualTo(1);
		assertThat(trips.seatMap(f.trip().getId()).occupied()).containsExactly(7);
	}

	@Test
	void annulationPlusDe24hAvant_rembourse80Pourcent_etLibereLesSieges() {
		var f = TestFixtures.create(catalog, auth, LocalDateTime.now().plusDays(3));
		Booking booking = bookAndPay(f, 5);

		Booking cancelled = bookings.cancel(f.client(), booking.getId());

		assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
		assertThat(cancelled.getRefundAmount()).isEqualTo(3200);
		assertThat(payments.forBooking(booking.getId())).first().extracting(Payment::getStatus)
				.isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
		assertThat(trips.seatMap(f.trip().getId()).occupied()).doesNotContain(5);
	}

	@Test
	void paiementIdempotent_memeCleMemeTransaction() {
		var f = TestFixtures.create(catalog, auth, LocalDateTime.now().plusDays(3));
		Booking booking = bookings.create(Customer.of(f.client()), f.trip().getId(), passengers(1), null);

		Payment first = payments.initiate(f.client(), booking.getId(), PaymentMethod.ORANGE_MONEY, "699112233", "cle-unique");
		Payment again = payments.initiate(f.client(), booking.getId(), PaymentMethod.ORANGE_MONEY, "699112233", "cle-unique");
		assertThat(again.getId()).isEqualTo(first.getId());

		// Webhook rejoué : le second appel est ignoré, pas de double confirmation
		payments.handleProviderResult(PaymentMethod.ORANGE_MONEY, first.getExternalReference(), true, null);
		payments.handleProviderResult(PaymentMethod.ORANGE_MONEY, first.getExternalReference(), true, null);
		assertThat(bookings.tickets(booking.getId())).hasSize(1);
	}

	@Test
	void numeroDunAutreOperateur_paiementEchoue() {
		var f = TestFixtures.create(catalog, auth, LocalDateTime.now().plusDays(3));
		Booking booking = bookings.create(Customer.of(f.client()), f.trip().getId(), passengers(2), null);

		Payment p = payments.initiate(f.client(), booking.getId(), PaymentMethod.MTN_MOMO, "699112233", null);

		assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
		assertThat(bookings.get(booking.getId()).getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
	}

	@Test
	void unAgentNePeutPasControlerLeBilletDuneAutreCompagnie() {
		var f1 = TestFixtures.create(catalog, auth, LocalDateTime.now().plusDays(3));
		var f2 = TestFixtures.create(catalog, auth, LocalDateTime.now().plusDays(3));
		Booking booking = bookAndPay(f1, 9);
		String qr = tickets.qrPayload(bookings.tickets(booking.getId()).get(0));

		var result = tickets.validate(qr, f2.agent());

		assertThat(result.valid()).isFalse();
		assertThat(result.message()).contains("autre compagnie");
	}
}
