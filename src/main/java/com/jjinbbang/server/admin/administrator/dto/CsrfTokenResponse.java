package com.jjinbbang.server.admin.administrator.dto;

import org.springframework.security.web.csrf.CsrfToken;

public record CsrfTokenResponse(
	String token,
	String parameterName,
	String headerName
) {

	public static CsrfTokenResponse from(CsrfToken csrfToken) {
		return new CsrfTokenResponse(
			csrfToken.getToken(),
			csrfToken.getParameterName(),
			csrfToken.getHeaderName()
		);
	}
}
