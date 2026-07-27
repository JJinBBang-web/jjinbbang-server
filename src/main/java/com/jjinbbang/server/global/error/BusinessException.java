package com.jjinbbang.server.global.error;

import lombok.Getter;

/**
 * 서비스 전체에서 쓰는 단 하나의 비즈니스 예외.
 *
 * <p>의미는 {@link ErrorCode}가 들고 있으므로 이 클래스를 상속할 필요가 없다.
 * 직접 {@code new} 하지 말고 {@code XxxErrorCode.CODE.exception()}으로 만든다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, Object... args) {
		super(String.format(errorCode.getMessage(), args));
		this.errorCode = errorCode;
	}
}
