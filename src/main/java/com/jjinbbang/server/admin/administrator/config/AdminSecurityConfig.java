package com.jjinbbang.server.admin.administrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.web.SecurityFilterChain;

import com.jjinbbang.server.admin.administrator.security.AdminAccessDeniedHandler;
import com.jjinbbang.server.admin.administrator.security.AdminAuthenticationEntryPoint;
import com.jjinbbang.server.admin.administrator.security.AdminOidcUserService;

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
			.csrf(Customizer.withDefaults());

		return http.build();
	}
}
