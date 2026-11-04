package cm.enspd.voyago.booking.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cm.enspd.voyago.booking.domain.RefundPolicy;

@Configuration
class BookingConfig {

	@Bean
	RefundPolicy refundPolicy(@Value("${voyago.refund.full-window-hours}") int fullHours,
			@Value("${voyago.refund.full-window-percent}") int fullPercent,
			@Value("${voyago.refund.partial-window-hours}") int partialHours,
			@Value("${voyago.refund.partial-window-percent}") int partialPercent) {
		return new RefundPolicy(fullHours, fullPercent, partialHours, partialPercent);
	}
}
