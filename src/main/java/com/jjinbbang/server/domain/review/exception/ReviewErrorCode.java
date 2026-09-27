package com.jjinbbang.server.domain.review.exception;

import org.springframework.http.HttpStatus;

import com.jjinbbang.server.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

	REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리뷰 정보가 존재하지 않습니다."),
	REVIEW_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 삭제된 리뷰입니다."),
	;

	private final HttpStatus status;
	private final String message;
}
