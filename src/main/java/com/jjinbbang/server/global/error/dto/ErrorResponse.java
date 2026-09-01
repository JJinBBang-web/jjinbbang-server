package com.jjinbbang.server.global.error.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jjinbbang.server.global.error.ErrorCode;

/**
 * 실패 응답 본문.
 *
 * <pre>
 * {
 *   "code": 404,
 *   "errorCode": "REVIEW_NOT_FOUND",
 *   "message": "해당 리뷰 정보가 존재하지 않습니다."
 * }
 * </pre>
 *
 * <p>{@code errors}는 검증 실패일 때만 붙는다. Ver.1은 첫 번째 필드 오류만 알려줬지만
 * 어드민 폼은 입력 항목이 많아 필드별로 전부 내려준다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"code", "errorCode", "message", "errors"})
public record ErrorResponse(
	int code,
	String errorCode,
	String message,
	List<FieldError> errors
) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.getStatus().value(), errorCode.getCode(), message, null);
	}

	public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> errors) {
		return new ErrorResponse(errorCode.getStatus().value(), errorCode.getCode(), message,
			errors == null || errors.isEmpty() ? null : errors);
	}
}
