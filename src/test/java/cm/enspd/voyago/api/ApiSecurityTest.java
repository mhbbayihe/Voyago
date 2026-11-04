package cm.enspd.voyago.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Sécurité de l'API REST : JWT, accès public et rôles. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityTest {

	@Autowired MockMvc mvc;

	@Test
	void rechercheEstPublique() throws Exception {
		mvc.perform(get("/api/v1/cities")).andExpect(status().isOk());
	}

	@Test
	void reservationsExigentUnJeton() throws Exception {
		mvc.perform(get("/api/v1/bookings")).andExpect(status().isUnauthorized());
	}

	@Test
	void inscriptionPuisConnexionDonneUnJwt() throws Exception {
		mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fullName\":\"Api User\",\"email\":\"api@test.cm\",\"password\":\"Password1\"}"))
				.andExpect(status().isCreated());
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"api@test.cm\",\"password\":\"Password1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.role").value("CLIENT"));
	}

	@Test
	void mauvaisMotDePasse_401() throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"inconnu@test.cm\",\"password\":\"Password1\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void webhookSansSignature_401() throws Exception {
		mvc.perform(post("/api/v1/payments/webhook/mtn").contentType(MediaType.APPLICATION_JSON)
				.content("{\"externalReference\":\"MOMO-X\",\"status\":\"SUCCESSFUL\"}"))
				.andExpect(status().isUnauthorized());
	}
}
