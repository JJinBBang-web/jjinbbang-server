package com.jjinbbang.server.admin.moderation.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 금칙어 활성화 / 비활성화 요청.
 *
 * <p>{@code boolean}이 아니라 {@code Boolean}인 것은 의도다 — 원시 타입이면 값을 안 보냈을 때
 * 조용히 {@code false}가 되어 "비활성화 요청"으로 둔갑한다.
 */
public record ProhibitedWordStatusUpdateRequest(

	@NotNull(message = "활성화 여부는 필수입니다.")
	Boolean enabled
) {
}
