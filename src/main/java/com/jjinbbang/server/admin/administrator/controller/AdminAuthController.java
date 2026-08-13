package com.jjinbbang.server.admin.administrator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jjinbbang.server.admin.administrator.dto.AdminSessionResponse;
import com.jjinbbang.server.admin.administrator.dto.CsrfTokenResponse;
import com.jjinbbang.server.admin.administrator.security.AdminOidcUser;
import com.jjinbbang.server.admin.administrator.service.AdminSessionService;
import com.jjinbbang.server.global.template.ResTemplate;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

	private final AdminSessionService adminSessionService;

	@GetMapping("/me")
	public ResTemplate<AdminSessionResponse> me(@AuthenticationPrincipal AdminOidcUser principal) {
		return new ResTemplate<>(
			HttpStatus.OK,
			"관리자 로그인 정보 조회 성공",
			adminSessionService.getSession(principal.getAdminId())
		);
	}

	@GetMapping("/csrf")
	public ResTemplate<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
		return new ResTemplate<>(
			HttpStatus.OK,
			"CSRF 토큰 조회 성공",
			CsrfTokenResponse.from(csrfToken)
		);
	}
}
