package com.jjinbbang.server.global.error;

import org.springframework.http.HttpStatus;

/**
 * 도메인별 ErrorCode enum이 구현하는 인터페이스.
 *
 * <p>Ver.1처럼 예외 클래스를 도메인마다 새로 만들지 않는다. 도메인은 이 인터페이스를 구현한
 * {@code XxxErrorCode} enum <b>파일 하나만</b> 두고, 던질 때는 {@link #exception()}을 쓴다.
 * (2026-05-10 백엔드 회의 · 2026-05-13 전체 회의 · 2026-07-26 허들 결정)
 *
 * <p>접근자 이름을 {@code getXxx}로 둔 이유는 Lombok {@code @Getter}가 그대로 구현을 채워주기 때문이다.
 * 덕분에 도메인 enum에는 <b>오버라이드가 한 줄도 없다.</b>
 *
 * <pre>
 * &#64;Getter
 * &#64;RequiredArgsConstructor
 * public enum ReviewErrorCode implements ErrorCode {
 *
 *     REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리뷰 정보가 존재하지 않습니다."),
 *     REVIEW_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 후기만 수정할 수 있습니다."),
 *     ;
 *
 *     private final HttpStatus status;
 *     private final String message;
 * }
 *
 * throw ReviewErrorCode.REVIEW_NOT_FOUND.exception();
 * </pre>
 */
public interface ErrorCode {

	/** 사용자에게 보여줄 한글 메시지. 인자를 받는 코드값은 {@code %s} 같은 형식 지정자를 둔다. */
	String getMessage();

	/** 이 코드값이 나갈 HTTP 상태. 상태를 호출부가 고르지 않게 하려고 코드값이 들고 있는다. */
	HttpStatus getStatus();

	/**
	 * 응답 본문의 {@code errorCode} 값.
	 *
	 * <p>enum 이름을 그대로 쓴다 — 코드값을 따로 문자열로 적으면 이름과 어긋나는 사고가 난다.
	 */
	default String getCode() {
		if (this instanceof Enum<?> constant) {
			return constant.name();
		}
		return getClass().getSimpleName();
	}

	/** 이 코드값으로 예외를 만든다. */
	default BusinessException exception() {
		return new BusinessException(this);
	}

	/**
	 * 메시지에 값을 채워 예외를 만든다.
	 *
	 * <pre>
	 * REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰(%s)를 찾을 수 없습니다.")
	 * throw ReviewErrorCode.REVIEW_NOT_FOUND.exception(reviewId);
	 * </pre>
	 */
	default BusinessException exception(Object... args) {
		return new BusinessException(this, args);
	}
}
