package com.jjinbbang.server.admin.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 금칙어 등록 요청.
 *
 * <p>{@code prohibited_words.word}가 {@code VARCHAR(255)}라 길이를 여기서 막는다 —
 * DB까지 내려가서 잘리거나 터지면 어떤 값이 문제인지 응답으로 알려줄 수 없다.
 */
public record ProhibitedWordCreateRequest(

	@NotBlank(message = "금칙어는 필수입니다.")
	@Size(max = 255, message = "금칙어는 255자를 넘을 수 없습니다.")
	String word
) {
}
