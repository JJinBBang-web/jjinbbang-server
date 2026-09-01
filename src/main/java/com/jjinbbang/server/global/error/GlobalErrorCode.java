package com.jjinbbang.server.global.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 도메인에 속하지 않는 공통 에러 코드. 프레임워크 단계에서 터지는 것들이 여기로 온다.
 *
 * <p>도메인 사유는 여기 넣지 않고 각 도메인의 {@code XxxErrorCode}에 둔다.
 * 이 파일이 도메인 enum 작성 예시이기도 하다 — 값만 적고 오버라이드는 없다.
 */
@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	INVALID_TYPE(HttpStatus.BAD_REQUEST, "요청 값의 형식이 올바르지 않습니다. (%s)"),
	ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 경로입니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
	;

	private final HttpStatus status;
	private final String message;
}
