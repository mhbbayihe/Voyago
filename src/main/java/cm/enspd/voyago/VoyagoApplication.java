package cm.enspd.voyago;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * VoyaGo - plateforme de réservation de billets de voyage (bus, train, avion).
 * Monolithe modulaire : chaque sous-package (auth, trip, booking, payment, notification, backoffice)
 * est un module indépendant découpé en couches api / application / domain / infrastructure.
 */
@SpringBootApplication
@EnableScheduling
public class VoyagoApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Africa/Douala"));
		SpringApplication.run(VoyagoApplication.class, args);
	}
}
