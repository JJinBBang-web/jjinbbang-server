package com.jjinbbang.server.admin.review.type;

/**
 * 어드민 리뷰 목록의 정렬. 프론트의 정렬 선택지와 같은 값을 받는다.
 *
 * <p>실제 정렬은 {@code ReviewSpecifications.orderBy}가 모든 기준을 한곳에서 건다.
 * 기준마다 {@code Sort}를 만들지 않는 이유는 {@code REPORT}가 {@code reports} 집계라
 * {@code Sort}로 표현할 수 없고, 정렬 방식이 둘로 갈리면 {@code Pageable}의 정렬이
 * Specification의 정렬을 덮어쓰는 문제가 생기기 때문이다.
 */
public enum ReviewSortType {
	LATEST,
	REPORT,
	RATING_HIGH,
	RATING_LOW
}
