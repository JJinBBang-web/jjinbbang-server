package com.jjinbbang.server.admin.administrator.exception;

import org.springframework.http.HttpStatus;

import com.jjinbbang.server.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminAuthenticationErrorCode implements ErrorCode {

	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "관리자 로그인이 필요합니다."),
	ADMIN_GROUP_REQUIRED(HttpStatus.FORBIDDEN, "관리자 그룹에 속한 계정만 접근할 수 있습니다."),
	ADMIN_DEACTIVATED(HttpStatus.FORBIDDEN, "비활성화된 관리자 계정입니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	;

	private final HttpStatus status;
	private final String message;
}
