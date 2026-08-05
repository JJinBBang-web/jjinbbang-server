package com.jjinbbang.server.global.paging;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 쿼리 파라미터로 받은 {@code page} · {@code size}를 {@link Pageable}로 바꾼다.
 *
 * <p>{@link PageRequest#of}는 음수 페이지나 0 이하의 크기를 받으면 {@code IllegalArgumentException}을 던진다.
 * 그대로 두면 {@code ?page=-1} 하나에 500이 나가므로 여기서 정상 범위로 눌러 담는다.
 * 형식 자체가 틀린 값({@code ?page=abc})은 그 전에 {@code MethodArgumentTypeMismatchException}으로
 * 걸려 400 {@code INVALID_TYPE}이 된다.
 *
 * <p>{@link #MAX_SIZE}를 두는 것은 {@code ?size=1000000} 한 번에 어드민 API가 멈추지 않게 하려는 것이다.
 */
public final class PageRequests {

	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;

	/**
	 * {@code @RequestParam(defaultValue = ...)} 에 넣을 {@link #DEFAULT_SIZE}.
	 *
	 * <p>애너테이션 속성은 컴파일 타임 상수만 받으므로 문자열이 따로 필요하다.
	 * 컨트롤러마다 {@code "20"} 을 적으면 {@link #DEFAULT_SIZE} 를 바꿔도 따라오지 않는다.
	 */
	public static final String DEFAULT_SIZE_PARAM = "" + DEFAULT_SIZE;

	private PageRequests() {
	}

	public static Pageable of(int page, int size, Sort sort) {
		return PageRequest.of(Math.max(page, 0), clampSize(size), sort);
	}

	private static int clampSize(int size) {
		if (size < 1) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}
}
