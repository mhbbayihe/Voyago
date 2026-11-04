package cm.enspd.voyago.booking.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QrCodeSignerTest {

	private final QrCodeSigner signer = new QrCodeSigner("secret");

	@Test
	void unQrSigneEstAccepte() {
		String payload = signer.payload("TK-ABCD1234");
		assertThat(signer.verify(payload)).contains("TK-ABCD1234");
	}

	@Test
	void unQrModifieEstRejete() {
		String payload = signer.payload("TK-ABCD1234").replace("TK-ABCD1234", "TK-ZZZZ9999");
		assertThat(signer.verify(payload)).isEmpty();
	}

	@Test
	void uneAutreCleNeValidePasLaSignature() {
		String payload = new QrCodeSigner("autre").payload("TK-ABCD1234");
		assertThat(signer.verify(payload)).isEmpty();
	}
}
