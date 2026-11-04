package cm.enspd.voyago.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import cm.enspd.voyago.shared.domain.BusinessException;
import cm.enspd.voyago.trip.domain.TransportMode;

/** Pattern State : transitions autorisées du cycle de vie d'une réservation. */
class BookingStatusTest {

	private Booking newBooking() {
		var trip = new BookedTrip(UUID.randomUUID(), UUID.randomUUID(), "Sanaga Express", TransportMode.BUS,
				"Douala → Yaoundé", "Mboppi", "Mvan", "Classique", LocalDateTime.now().plusDays(2), 4000, 40);
		Booking b = Booking.create(UUID.randomUUID(), "Client", "c@x.cm", null, trip, LocalDateTime.now(), 15);
		b.addPassenger("Jean", "Kamga", "CNI1", 1);
		b.addPassenger("Marie", "Ngo", "CNI2", 2);
		return b;
	}

	@Test
	void montantTotal_estPrixUnitaireFoisPassagers() {
		assertThat(newBooking().getTotalAmount()).isEqualTo(8000);
	}

	@Test
	void transitionsAutorisees() {
		assertThat(BookingStatus.PENDING_PAYMENT.canTransitionTo(BookingStatus.CONFIRMED)).isTrue();
		assertThat(BookingStatus.PENDING_PAYMENT.canTransitionTo(BookingStatus.EXPIRED)).isTrue();
		assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.CANCELLED)).isTrue();
		assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.EXPIRED)).isFalse();
		assertThat(BookingStatus.EXPIRED.next()).isEmpty();
		assertThat(BookingStatus.CANCELLED.next()).isEmpty();
	}

	@Test
	void uneReservationExpireeNePeutPasEtreConfirmee() {
		Booking b = newBooking();
		b.expire();
		assertThatThrownBy(() -> b.confirm(LocalDateTime.now())).isInstanceOf(BusinessException.class);
	}

	@Test
	void verrouExpireApres15Minutes() {
		Booking b = newBooking();
		assertThat(b.isPayable(LocalDateTime.now())).isTrue();
		assertThat(b.isLockExpired(LocalDateTime.now().plusMinutes(16))).isTrue();
	}
}
