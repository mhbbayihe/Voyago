package cm.enspd.voyago.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** RG5 : barème d'annulation. */
class RefundPolicyTest {

	private final RefundPolicy policy = new RefundPolicy(24, 80, 2, 50);
	private final LocalDateTime now = LocalDateTime.of(2026, 11, 4, 12, 0);

	@Test
	void plusDe24hAvantLeDepart_rembourse80Pourcent() {
		var quote = policy.quote(10_000, now.plusHours(48), now);
		assertThat(quote.percent()).isEqualTo(80);
		assertThat(quote.amount()).isEqualTo(8_000);
	}

	@Test
	void entre24het2h_rembourse50Pourcent() {
		assertThat(policy.quote(10_000, now.plusHours(10), now).amount()).isEqualTo(5_000);
		assertThat(policy.quote(10_000, now.plusHours(24), now).percent()).isEqualTo(50);
		assertThat(policy.quote(10_000, now.plusHours(2), now).percent()).isEqualTo(50);
	}

	@Test
	void moinsDe2h_aucunRemboursement() {
		var quote = policy.quote(10_000, now.plusMinutes(90), now);
		assertThat(quote.percent()).isZero();
		assertThat(quote.amount()).isZero();
	}
}
