package com.jjinbbang.server.admin.administrator.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import com.jjinbbang.server.admin.administrator.security.AdminAccessDeniedHandler;
import com.jjinbbang.server.admin.administrator.security.AdminAuthenticationEntryPoint;
import com.jjinbbang.server.admin.administrator.security.AdminOidcUserService;
import com.jjinbbang.server.admin.administrator.security.AdminSessionValidationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
	prefix = "app.auth",
	name = "oidc-enabled",
	havingValue = "true"
)
public class AdminSecurityConfig {

	private final AdminAuthenticationEntryPoint authenticationEntryPoint;
	private final AdminAccessDeniedHandler accessDeniedHandler;
	private final AdminOidcUserService oidcUserService;
	private final AdminSessionValidationFilter sessionValidationFilter;

	@Bean
	JwtDecoderFactory<ClientRegistration> adminJwtDecoderFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(5));
		requestFactory.setReadTimeout(Duration.ofSeconds(5));
		RestTemplate restOperations = new RestTemplate(requestFactory);

		return clientRegistration -> {
			NimbusJwtDecoder decoder = NimbusJwtDecoder
				.withJwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri())
				.restOperations(restOperations)
				.build();
			decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
				new OidcIdTokenValidator(clientRegistration)));
			decoder.setClaimSetConverter(OidcIdTokenDecoderFactory.createDefaultClaimTypeConverter());
			return decoder;
		};
	}

	@Bean
	SecurityFilterChain adminSecurityFilterChain(
		HttpSecurity http,
		ClientRegistrationRepository clientRegistrationRepository,
		@Value("${app.auth.login-success-uri:/api/admin/auth/me}") String loginSuccessUri
	) throws Exception {
		OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
			new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
		logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/login?logout");

		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health", "/actuator/health/**", "/oauth2/**", "/login/**", "/error")
				.permitAll()
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.anyRequest().denyAll()
			)
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
				.defaultSuccessUrl(loginSuccessUri, true)
				.failureUrl("/login?error=authentication_failed")
			)
				.logout(logout -> logout
				.logoutUrl("/api/admin/auth/logout")
				.logoutSuccessHandler(logoutSuccessHandler)
				.invalidateHttpSession(true)
				.clearAuthentication(true)
				.deleteCookies("JJINBBANG_ADMIN_SESSION")
				)
				.addFilterAfter(sessionValidationFilter, OAuth2LoginAuthenticationFilter.class)
				.csrf(Customizer.withDefaults());

		return http.build();
	}
}
