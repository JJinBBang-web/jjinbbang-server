package com.jjinbbang.server.admin.review.dto.response;

import java.time.LocalDateTime;

import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.domain.review.type.ReviewStatus;

/**
 * 리뷰 한 건.
 *
 * <p>스키마 때문에 명세서와 달라진 곳이 둘 있다.
 * <ul>
 *   <li>{@code reviews}에 제목 컬럼이 없어 title은 내려주지 않고 {@code content}만 내려준다.</li>
 *   <li>{@code reviews.rating}이 정수(INT)라 소수점 없이 그대로 나간다.</li>
 * </ul>
 */
public record ReviewResponse(
	Long id,
	ReviewStatus status,
	boolean hasBadWord,
	String schoolName,
	String content,
	String userName,
	Integer rating,
	long reportCount,
	LocalDateTime createdAt
) {

	public static ReviewResponse of(Review review, long reportCount) {
		return new ReviewResponse(
			review.getId(),
			review.getStatus(),
			review.isProhibitedWordFlag(),
			review.getUser().getUniversity().getName(),
			review.getContent(),
			review.getUser().getNickname(),
			review.getRating(),
			reportCount,
			review.getCreatedAt()
		);
	}
}
