package com.jjinbbang.server.admin.review.dto.request;

/** 마스킹 해제 요청. 본문 자체가 선택이며, 해제 사유({@code detailReason})도 선택이다. */
public record ReviewUnmaskRequest(
	String detailReason
) {
}
