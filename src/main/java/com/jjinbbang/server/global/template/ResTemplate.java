package com.jjinbbang.server.global.template;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 성공 응답 래퍼. Ver.1(`JJinBBang_BE`)의 규약을 그대로 물려받는다.
 *
 * <pre>
 * { "code": 200, "message": "리뷰 불러오기 성공", "data": { ... } }
 * </pre>
 *
 * <p>컨트롤러는 <b>항상</b> 이걸로 감싸서 반환하고 {@code message}에는 한글 성공 문구를 직접 넣는다.
 * 내려줄 데이터가 없으면 {@code ResTemplate<Void>}에 {@code data}를 {@code null}로 둔다.
 */
@Slf4j
@Getter
@JsonPropertyOrder({"code", "message", "data"})
public class ResTemplate<T> {

	private final int code;
	private final String message;
	private final T data;

	public ResTemplate(HttpStatus httpStatus, String message, T data) {
		this.code = httpStatus.value();
		this.message = message;
		this.data = data;
		log.info("Response generated: code={}, message={}", code, message);
	}

	public ResTemplate(HttpStatus httpStatus, String message) {
		this(httpStatus, message, null);
	}
}
