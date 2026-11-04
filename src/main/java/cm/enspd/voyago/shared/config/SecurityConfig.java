package cm.enspd.voyago.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import cm.enspd.voyago.auth.application.AppUserDetailsService;
import cm.enspd.voyago.auth.application.JwtService;
import cm.enspd.voyago.auth.infrastructure.JwtAuthenticationFilter;

/**
 * Deux chaînes de sécurité :
 * 1. /api/** : API REST sans état, authentifiée par JWT (pour une future application mobile ou SPA) ;
 * 2. le reste : site web Thymeleaf avec session, formulaire de connexion et protection CSRF.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	@Order(1)
	SecurityFilterChain apiChain(HttpSecurity http, JwtService jwtService, AppUserDetailsService users) throws Exception {
		http.securityMatcher("/api/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/trips/**", "/api/v1/cities").permitAll()
						.requestMatchers("/api/v1/payments/webhook/**").permitAll()
						.requestMatchers("/api/v1/tickets/validate", "/api/v1/admin/**").hasAnyRole("AGENT", "ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.addFilterBefore(new JwtAuthenticationFilter(jwtService, users), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain webChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/search", "/register", "/login", "/about", "/error").permitAll()
				.requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health").permitAll()
				.requestMatchers("/h2-console/**").hasRole("ADMIN")
				.requestMatchers("/agent/**").hasRole("AGENT")
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated())
				.formLogin(form -> form.loginPage("/login").usernameParameter("email")
						.defaultSuccessUrl("/dashboard", false).failureUrl("/login?error").permitAll())
				.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/?logout"))
				.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
				.headers(h -> h.frameOptions(f -> f.sameOrigin())
						.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
		return http.build();
	}
}
