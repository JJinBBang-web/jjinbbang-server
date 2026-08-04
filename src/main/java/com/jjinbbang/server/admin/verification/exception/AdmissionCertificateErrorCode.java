package com.jjinbbang.server.admin.verification.exception;

import com.jjinbbang.server.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdmissionCertificateErrorCode implements ErrorCode {

	ADMISSION_CERTIFICATE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 합격증명서를 찾을 수 없습니다."),
	ADMISSION_CERTIFICATE_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 합격증명서입니다."),
	;

	private final HttpStatus status;
	private final String message;
}
