package com.jjinbbang.server.admin.administrator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.jjinbbang.server.admin.administrator.security.AdminAccessDeniedHandler;
import com.jjinbbang.server.admin.administrator.security.AdminAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
	prefix = "app.auth",
	name = "oidc-enabled",
	havingValue = "false",
	matchIfMissing = true
)
public class FallbackSecurityConfig {

	private final AdminAuthenticationEntryPoint authenticationEntryPoint;
	private final AdminAccessDeniedHandler accessDeniedHandler;

	@Bean
	SecurityFilterChain fallbackSecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
				.anyRequest().denyAll()
			)
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			);

		return http.build();
	}
}
