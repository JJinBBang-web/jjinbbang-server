package com.jjinbbang.server.admin.review.dto.response;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.global.paging.PageInfo;

public record ReviewListResponse(
	List<ReviewResponse> reviewList,
	PageInfo pageInfo
) {

	public static ReviewListResponse of(Page<Review> page, Map<Long, Long> reportCounts) {
		List<ReviewResponse> reviewList = page.getContent().stream()
			.map(review -> ReviewResponse.of(review, reportCounts.getOrDefault(review.getId(), 0L)))
			.toList();

		return new ReviewListResponse(reviewList, PageInfo.from(page));
	}
}
