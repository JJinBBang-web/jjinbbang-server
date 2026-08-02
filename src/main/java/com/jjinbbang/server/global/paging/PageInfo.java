package com.jjinbbang.server.global.paging;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 목록 응답에 함께 나가는 페이지 정보.
 *
 * <pre>
 * { "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
 * </pre>
 *
 * <p>{@code data}에 배열을 바로 내리지 않고 {@code { xxxList, pageInfo }}로 감싸는 이유는,
 * 나중에 집계 값을 하나 더 붙일 때 응답 구조를 바꾸지 않아도 되기 때문이다.
 *
 * <p>{@code page}는 0-base다 — Spring Data의 {@link Page#getNumber()}를 그대로 쓴다.
 */
@JsonPropertyOrder({"page", "size", "totalElements", "totalPages"})
public record PageInfo(
	int page,
	int size,
	long totalElements,
	int totalPages
) {

	public static PageInfo from(Page<?> page) {
		return new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
