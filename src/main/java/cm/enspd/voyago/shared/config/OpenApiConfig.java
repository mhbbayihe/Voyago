package cm.enspd.voyago.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI voyagoOpenApi() {
		return new OpenAPI()
				.info(new Info().title("VoyaGo API").version("v1")
						.description("API REST de la plateforme de réservation de billets VoyaGo (bus, train, avion). "
								+ "Obtenez un jeton via POST /api/v1/auth/login puis cliquez sur « Authorize »."))
				.components(new Components().addSecuritySchemes("bearer",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList("bearer"));
	}
}
