package com.jjinbbang.server.global.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 테스트용 도메인 ErrorCode. 실제 도메인이 만들 파일이 어떤 모양인지 그대로 보여주는 역할도 한다.
 */
@Getter
@RequiredArgsConstructor
public enum TestErrorCode implements ErrorCode {

	SAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "샘플을 찾을 수 없습니다."),
	SAMPLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 샘플만 수정할 수 있습니다."),
	SAMPLE_NOT_FOUND_WITH_ID(HttpStatus.NOT_FOUND, "샘플(%s)을 찾을 수 없습니다."),
	SAMPLE_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "샘플 처리 중 오류가 발생했습니다."),
	;

	private final HttpStatus status;
	private final String message;
}
