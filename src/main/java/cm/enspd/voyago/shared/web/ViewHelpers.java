package cm.enspd.voyago.shared.web;

import java.util.Locale;

import org.springframework.stereotype.Component;

/** Aides de formatage utilisées dans les gabarits Thymeleaf via ${@fmt.money(...)}. */
@Component("fmt")
public class ViewHelpers {

	public String money(long amount) {
		return String.format(Locale.FRENCH, "%,d", amount).replace(' ', ' ').replace(' ', ' ') + " FCFA";
	}

	public String duration(int minutes) {
		return (minutes / 60) + " h " + String.format("%02d", minutes % 60);
	}
}
