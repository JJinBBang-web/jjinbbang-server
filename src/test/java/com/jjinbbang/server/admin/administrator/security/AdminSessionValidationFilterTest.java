package com.jjinbbang.server.admin.administrator.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import com.jjinbbang.server.admin.administrator.exception.AdminAuthenticationErrorCode;
import com.jjinbbang.server.admin.administrator.repository.AdminRepository;
import com.jjinbbang.server.admin.administrator.type.AdminStatus;

import jakarta.servlet.FilterChain;

@DisplayName("관리자 세션 유효성 검증")
class AdminSessionValidationFilterTest {

	private static final String REQUIRED_GROUP = "jjinbbang-backoffice-admins";

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("활성 관리자와 유효한 OIDC 토큰은 요청을 계속 처리한다")
	void activeAdminContinuesRequest() throws Exception {
		AdminRepository repository = mock(AdminRepository.class);
		SecurityErrorResponseWriter responseWriter = mock(SecurityErrorResponseWriter.class);
		FilterChain filterChain = mock(FilterChain.class);
		AdminOidcUser principal = principal(Instant.now().plusSeconds(300));
		authenticate(principal);
		when(repository.existsByIdAndStatus(7L, AdminStatus.ACTIVE)).thenReturn(true);

		AdminSessionValidationFilter filter =
			new AdminSessionValidationFilter(repository, responseWriter, REQUIRED_GROUP);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/auth/me");
		request.setServletPath("/api/admin/auth/me");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(responseWriter, never()).write(any(), any());
	}

	@Test
	@DisplayName("비활성화된 관리자는 기존 세션을 즉시 폐기한다")
	void deactivatedAdminInvalidatesSession() throws Exception {
		AdminRepository repository = mock(AdminRepository.class);
		SecurityErrorResponseWriter responseWriter = mock(SecurityErrorResponseWriter.class);
		FilterChain filterChain = mock(FilterChain.class);
		AdminOidcUser principal = principal(Instant.now().plusSeconds(300));
		authenticate(principal);
		when(repository.existsByIdAndStatus(7L, AdminStatus.ACTIVE)).thenReturn(false);

		AdminSessionValidationFilter filter =
			new AdminSessionValidationFilter(repository, responseWriter, REQUIRED_GROUP);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/auth/me");
		request.setServletPath("/api/admin/auth/me");
		MockHttpSession session = new MockHttpSession();
		request.setSession(session);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(responseWriter).write(response, AdminAuthenticationErrorCode.ADMIN_DEACTIVATED);
		verify(filterChain, never()).doFilter(any(), any());
		org.assertj.core.api.Assertions.assertThat(session.isInvalid()).isTrue();
	}

	@Test
	@DisplayName("OIDC 토큰이 만료되면 재로그인을 요구하고 기존 세션을 폐기한다")
	void expiredTokenInvalidatesSession() throws Exception {
		AdminRepository repository = mock(AdminRepository.class);
		SecurityErrorResponseWriter responseWriter = mock(SecurityErrorResponseWriter.class);
		FilterChain filterChain = mock(FilterChain.class);
		AdminOidcUser principal = principal(Instant.now().minusSeconds(1));
		authenticate(principal);
		when(repository.existsByIdAndStatus(7L, AdminStatus.ACTIVE)).thenReturn(true);

		AdminSessionValidationFilter filter =
			new AdminSessionValidationFilter(repository, responseWriter, REQUIRED_GROUP);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/auth/me");
		request.setServletPath("/api/admin/auth/me");
		MockHttpSession session = new MockHttpSession();
		request.setSession(session);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(responseWriter).write(response, AdminAuthenticationErrorCode.AUTHENTICATION_REQUIRED);
		verify(filterChain, never()).doFilter(any(), any());
		org.assertj.core.api.Assertions.assertThat(session.isInvalid()).isTrue();
	}

	private void authenticate(AdminOidcUser principal) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(
				principal,
				"n/a",
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
			)
		);
	}

	private AdminOidcUser principal(Instant expiresAt) {
		AdminOidcUser principal = mock(AdminOidcUser.class);
		Instant issuedAt = expiresAt.minusSeconds(300);
		OidcIdToken idToken = new OidcIdToken(
			"token",
			issuedAt,
			expiresAt,
			Map.of("sub", "subject", "groups", List.of(REQUIRED_GROUP))
		);
		when(principal.getAdminId()).thenReturn(7L);
		when(principal.getClaims()).thenReturn(Map.of("groups", List.of(REQUIRED_GROUP)));
		when(principal.getIdToken()).thenReturn(idToken);
		return principal;
	}
}
