package com.jjinbbang.server.admin.administrator.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jjinbbang.server.admin.administrator.exception.AdminAuthenticationErrorCode;
import com.jjinbbang.server.admin.administrator.repository.AdminRepository;
import com.jjinbbang.server.admin.administrator.type.AdminStatus;
import com.jjinbbang.server.global.error.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AdminSessionValidationFilter extends OncePerRequestFilter {

	private static final String ADMIN_API_PREFIX = "/api/admin/";
	private static final String GROUPS_CLAIM = "groups";

	private final AdminRepository adminRepository;
	private final SecurityErrorResponseWriter responseWriter;
	private final String requiredGroup;

	public AdminSessionValidationFilter(
		AdminRepository adminRepository,
		SecurityErrorResponseWriter responseWriter,
		@Value("${app.auth.admin-group:jjinbbang-backoffice-admins}") String requiredGroup
	) {
		this.adminRepository = adminRepository;
		this.responseWriter = responseWriter;
		this.requiredGroup = requiredGroup;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getServletPath().startsWith(ADMIN_API_PREFIX);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AdminOidcUser principal)) {
			filterChain.doFilter(request, response);
			return;
		}

		ErrorCode rejection = validate(principal);
		if (rejection == null) {
			filterChain.doFilter(request, response);
			return;
		}

		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		responseWriter.write(response, rejection);
	}

	private ErrorCode validate(AdminOidcUser principal) {
		if (!adminRepository.existsByIdAndStatus(principal.getAdminId(), AdminStatus.ACTIVE)) {
			return AdminAuthenticationErrorCode.ADMIN_DEACTIVATED;
		}

		Object groups = principal.getClaims().get(GROUPS_CLAIM);
		if (!(groups instanceof Collection<?> values) || !values.contains(requiredGroup)) {
			return AdminAuthenticationErrorCode.ADMIN_GROUP_REQUIRED;
		}

		Instant expiresAt = principal.getIdToken().getExpiresAt();
		if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
			return AdminAuthenticationErrorCode.AUTHENTICATION_REQUIRED;
		}
		return null;
	}
}
