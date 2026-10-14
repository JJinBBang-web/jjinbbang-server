package com.jjinbbang.server.admin.review.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jjinbbang.server.admin.administrator.security.AdminOidcUser;
import com.jjinbbang.server.admin.review.dto.request.ReviewMaskRequest;
import com.jjinbbang.server.admin.review.dto.request.ReviewUnmaskRequest;
import com.jjinbbang.server.admin.review.dto.request.ReviewStatusUpdateRequest;
import com.jjinbbang.server.admin.review.dto.response.ReviewDetailResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewMaskPreviewResponse;
import com.jjinbbang.server.admin.review.service.ReviewService;
import com.jjinbbang.server.admin.review.type.ReviewPeriodType;
import com.jjinbbang.server.admin.review.type.ReviewSortType;
import com.jjinbbang.server.domain.review.type.ReviewStatus;
import com.jjinbbang.server.global.paging.PageRequests;
import com.jjinbbang.server.global.template.ResTemplate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 리뷰 관리. 어드민 웹의 "리뷰 관리" 화면이 호출한다.
 *
 * <p>정렬은 기준({@code sort})과 방향({@code direction})을 따로 받는다. 정렬은 서비스가
 * Specification으로 걸기 때문에 {@code Pageable}에는 페이지 번호와 크기만 담아 넘긴다.
 * 두 값 모두 enum 이름 그대로(대문자) 받으며, 그 밖의 값은 400 {@code INVALID_TYPE}이 된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	@GetMapping
	public ResTemplate<ReviewListResponse> findAll(
		@RequestParam(name = "searchKeyword", required = false) String searchKeyword,
		@RequestParam(name = "schoolNames", required = false) List<String> schoolNames,
		@RequestParam(name = "periodType", required = false) ReviewPeriodType periodType,
		@RequestParam(name = "status", required = false) ReviewStatus status,
		@RequestParam(name = "hasBadWordOnly", defaultValue = "false") boolean hasBadWordOnly,
		@RequestParam(name = "sort", defaultValue = "LATEST") ReviewSortType sort,
		@RequestParam(name = "direction", defaultValue = "DESC") Sort.Direction direction,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = PageRequests.DEFAULT_SIZE_PARAM) int size
	) {
		ReviewListResponse response = reviewService.findAll(
			searchKeyword,
			schoolNames,
			periodType,
			status,
			hasBadWordOnly,
			sort,
			direction,
			PageRequests.of(page, size, Sort.unsorted())
		);

		return new ResTemplate<>(HttpStatus.OK, "리뷰 목록 조회 성공", response);
	}

	@GetMapping("/{reviewId}")
	public ResTemplate<ReviewDetailResponse> findById(@PathVariable("reviewId") Long reviewId) {
		return new ResTemplate<>(HttpStatus.OK, "리뷰 상세 조회 성공", reviewService.findById(reviewId));
	}

	@DeleteMapping("/{reviewId}")
	public ResTemplate<Void> delete(@PathVariable("reviewId") Long reviewId) {
		reviewService.delete(reviewId);
		return new ResTemplate<>(HttpStatus.OK, "리뷰 삭제 성공");
	}

	@PatchMapping("/{reviewId}/status")
	public ResTemplate<Void> updateStatus(
		@PathVariable("reviewId") Long reviewId,
		@Valid @RequestBody ReviewStatusUpdateRequest request
	) {
		reviewService.updateStatus(reviewId, request.status());
		return new ResTemplate<>(HttpStatus.OK, "리뷰 상태 변경 성공");
	}

	@GetMapping("/{reviewId}/mask")
	public ResTemplate<ReviewMaskPreviewResponse> previewMask(@PathVariable("reviewId") Long reviewId) {
		return new ResTemplate<>(HttpStatus.OK, "리뷰 마스킹 미리보기 조회 성공", reviewService.previewMask(reviewId));
	}

	@PostMapping("/{reviewId}/mask")
	public ResTemplate<Void> mask(
		@PathVariable("reviewId") Long reviewId,
		@Valid @RequestBody ReviewMaskRequest request,
		@AuthenticationPrincipal AdminOidcUser principal
	) {
		reviewService.mask(reviewId, principal.getAdminId(), request.reasons(), request.detailReason());
		return new ResTemplate<>(HttpStatus.OK, "리뷰 마스킹 성공");
	}

	@PostMapping("/{reviewId}/unmask")
	public ResTemplate<Void> unmask(
		@PathVariable("reviewId") Long reviewId,
		@RequestBody(required = false) ReviewUnmaskRequest request,
		@AuthenticationPrincipal AdminOidcUser principal
	) {
		String detailReason = request == null ? null : request.detailReason();
		reviewService.unmask(reviewId, principal.getAdminId(), detailReason);
		return new ResTemplate<>(HttpStatus.OK, "리뷰 마스킹 해제 성공");
	}
}
