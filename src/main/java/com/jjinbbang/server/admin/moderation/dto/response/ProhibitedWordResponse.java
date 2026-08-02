package com.jjinbbang.server.admin.moderation.dto.response;

import java.time.LocalDateTime;

import com.jjinbbang.server.admin.moderation.entity.ProhibitedWord;

/**
 * 금칙어 한 건. 등록 · 목록 · 상태 변경이 모두 이 형태를 쓴다.
 *
 * <p>필드 이름이 DB와 다른 곳은 {@code wordId}({@code id})와 {@code enabled}({@code is_enabled})뿐이다.
 */
public record ProhibitedWordResponse(
	Long wordId,
	String word,
	Boolean enabled,
	LocalDateTime createdAt
) {

	public static ProhibitedWordResponse from(ProhibitedWord prohibitedWord) {
		return new ProhibitedWordResponse(
			prohibitedWord.getId(),
			prohibitedWord.getWord(),
			prohibitedWord.getEnabled(),
			prohibitedWord.getCreatedAt()
		);
	}
}
