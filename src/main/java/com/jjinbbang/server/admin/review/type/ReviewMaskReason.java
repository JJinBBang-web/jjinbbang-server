package com.jjinbbang.server.admin.review.type;

import java.util.List;
import java.util.stream.Collectors;

public enum ReviewMaskReason {
	BAD_WORD,
	PRIVACY_EXPOSURE,
	FALSE_SUSPICION,
	ETC;

	/**
	 * 여러 사유를 {@code ActionHistory.reason}에 저장할 하나의 문자열로 합친다.
	 *
	 * <p>{@code ActionHistory.reason}은 신고 기각 등 다른 조치도 함께 쓰는 범용 컬럼이라
	 * 타입을 {@code List<ReviewMaskReason>}으로 바꾸지 않는다 — 여러 사유를 문자열로 합치고
	 * 푸는 책임은 이 enum이 진다.
	 */
	public static String join(List<ReviewMaskReason> reasons) {
		return reasons.stream().map(Enum::name).collect(Collectors.joining(","));
	}
}
