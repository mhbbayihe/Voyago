package cm.enspd.voyago.shared.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

@Configuration
public class WebConfig {

	/** Formats locaux : dates en français. */
	@Bean
	LocaleResolver localeResolver() {
		return new FixedLocaleResolver(Locale.FRENCH);
	}
}
