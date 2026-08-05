package com.jjinbbang.server.admin.moderation.exception;

import org.springframework.http.HttpStatus;

import com.jjinbbang.server.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 신고·금칙어 도메인의 에러 코드. 이 도메인이 추가하는 예외 파일은 <b>이것 하나</b>다.
 *
 * <p>예외 클래스를 따로 만들지 않고 {@code ModerationErrorCode.XXX.exception()}으로 던진다.
 * 상태는 코드값이 들고 있으므로 호출부가 고르지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum ModerationErrorCode implements ErrorCode {

	PROHIBITED_WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 금칙어 정보가 존재하지 않습니다."),
	DUPLICATE_PROHIBITED_WORD(HttpStatus.CONFLICT, "이미 등록된 금칙어입니다. (%s)"),
	REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 신고 정보가 존재하지 않습니다."),
	REPORT_ALREADY_HANDLED(HttpStatus.CONFLICT, "이미 처리된 신고입니다."),
	;

	private final HttpStatus status;
	private final String message;
}
