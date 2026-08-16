package com.jjinbbang.server.admin.review.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjinbbang.server.admin.administrator.entity.ActionHistory;
import com.jjinbbang.server.admin.administrator.repository.ActionHistoryRepository;
import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.repository.ProhibitedWordFlagRepository;
import com.jjinbbang.server.admin.moderation.repository.ReportRepository;
import com.jjinbbang.server.admin.review.dto.response.ReviewDetailResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.repository.ReviewSpecifications;
import com.jjinbbang.server.admin.review.type.ReviewPeriodType;
import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.domain.review.exception.ReviewErrorCode;
import com.jjinbbang.server.domain.review.repository.ReviewImageRepository;
import com.jjinbbang.server.domain.review.repository.ReviewRepository;
import com.jjinbbang.server.domain.review.type.ReviewStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final ReviewImageRepository reviewImageRepository;
	private final ReportRepository reportRepository;
	private final ProhibitedWordFlagRepository prohibitedWordFlagRepository;
	private final ActionHistoryRepository actionHistoryRepository;

	public ReviewListResponse findAll(
		String searchKeyword,
		List<String> schoolNames,
		ReviewPeriodType periodType,
		ReviewStatus status,
		boolean hasBadWordOnly,
		Pageable pageable
	) {
		LocalDateTime createdAfter = periodType == null ? null : periodType.resolveCreatedAfter(LocalDateTime.now());

		Specification<Review> spec = ReviewSpecifications.withFilters(
			searchKeyword,
			schoolNames,
			status,
			createdAfter,
			hasBadWordOnly
		);

		Page<Review> page = reviewRepository.findAll(spec, pageable);

		List<Long> reviewIds = page.getContent().stream().map(review -> review.getId()).toList();

		Set<Long> badWordReviewIds = reviewIds.isEmpty()
			? Set.of()
			: new HashSet<>(prohibitedWordFlagRepository.findFlaggedReviewIds(reviewIds));

		Map<Long, Long> reportCounts = reviewIds.isEmpty()
			? Map.of()
			: reportRepository.countByReviewIdIn(reviewIds).stream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

		return ReviewListResponse.of(page, badWordReviewIds, reportCounts);
	}

	public ReviewDetailResponse findById(Long reviewId) {
		Review review = reviewRepository.findDetailById(reviewId)
			.orElseThrow(ReviewErrorCode.REVIEW_NOT_FOUND::exception);

		boolean hasBadWordFlag = prohibitedWordFlagRepository.existsByReviewId(reviewId);
		List<String> images = reviewImageRepository.findByReviewIdOrderBySortOrderAsc(reviewId).stream()
			.map(image -> image.getUrl())
			.toList();
		List<Report> reports = reportRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);
		List<ActionHistory> actionHistories = actionHistoryRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);

		return ReviewDetailResponse.of(review, hasBadWordFlag, images, reports, actionHistories);
	}

	@Transactional
	public void delete(Long reviewId) {
		Review review = reviewRepository.findById(reviewId)
			.orElseThrow(ReviewErrorCode.REVIEW_NOT_FOUND::exception);

		if (review.isDeleted()) {
			throw ReviewErrorCode.REVIEW_ALREADY_DELETED.exception();
		}

		review.delete();
	}
}
