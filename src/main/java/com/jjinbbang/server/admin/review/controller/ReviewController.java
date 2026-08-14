package com.jjinbbang.server.admin.review.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.service.ReviewService;
import com.jjinbbang.server.admin.review.type.ReviewPeriodType;
import com.jjinbbang.server.domain.review.type.ReviewStatus;
import com.jjinbbang.server.global.paging.PageRequests;
import com.jjinbbang.server.global.template.ResTemplate;

import lombok.RequiredArgsConstructor;

/**
 * 리뷰 관리. 어드민 웹의 "리뷰 관리" 화면이 호출한다.
 *
 * <p>{@code sort}는 현재 {@code LATEST} 하나뿐이라 파라미터로 받지 않고 최신순으로 고정한다.
 * 명세서에 다른 정렬 기준이 추가되면 그때 파라미터로 뺀다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
public class ReviewController {

	private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

	private final ReviewService reviewService;

	@GetMapping
	public ResTemplate<ReviewListResponse> findAll(
		@RequestParam(name = "searchKeyword", required = false) String searchKeyword,
		@RequestParam(name = "schoolNames", required = false) List<String> schoolNames,
		@RequestParam(name = "periodType", required = false) ReviewPeriodType periodType,
		@RequestParam(name = "status", required = false) ReviewStatus status,
		@RequestParam(name = "hasBadWordOnly", defaultValue = "false") boolean hasBadWordOnly,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = PageRequests.DEFAULT_SIZE_PARAM) int size
	) {
		ReviewListResponse response = reviewService.findAll(
			searchKeyword,
			schoolNames,
			periodType,
			status,
			hasBadWordOnly,
			PageRequests.of(page, size, LATEST_FIRST)
		);

		return new ResTemplate<>(HttpStatus.OK, "리뷰 목록 조회 성공", response);
	}
}
