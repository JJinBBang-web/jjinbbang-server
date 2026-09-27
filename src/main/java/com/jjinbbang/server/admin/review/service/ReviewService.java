package com.jjinbbang.server.admin.review.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjinbbang.server.admin.administrator.entity.Admin;
import com.jjinbbang.server.admin.administrator.entity.ActionHistory;
import com.jjinbbang.server.admin.administrator.repository.ActionHistoryRepository;
import com.jjinbbang.server.admin.administrator.repository.AdminRepository;
import com.jjinbbang.server.admin.moderation.entity.ProhibitedWord;
import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.repository.ProhibitedWordRepository;
import com.jjinbbang.server.admin.moderation.repository.ReportRepository;
import com.jjinbbang.server.admin.review.dto.response.ReviewDetailResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewMaskPreviewResponse;
import com.jjinbbang.server.admin.review.repository.ReviewSpecifications;
import com.jjinbbang.server.admin.review.type.ReviewMaskReason;
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
	private final ProhibitedWordRepository prohibitedWordRepository;
	private final ActionHistoryRepository actionHistoryRepository;
	private final AdminRepository adminRepository;

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

		Map<Long, Long> reportCounts = reviewIds.isEmpty()
			? Map.of()
			: reportRepository.countByReviewIdIn(reviewIds).stream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

		return ReviewListResponse.of(page, reportCounts);
	}

	public ReviewDetailResponse findById(Long reviewId) {
		Review review = reviewRepository.findDetailById(reviewId)
			.orElseThrow(ReviewErrorCode.REVIEW_NOT_FOUND::exception);

		List<String> images = reviewImageRepository.findByReviewIdOrderBySortOrderAsc(reviewId).stream()
			.map(image -> image.getUrl())
			.toList();
		List<Report> reports = reportRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);
		List<ActionHistory> actionHistories = actionHistoryRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);

		return ReviewDetailResponse.of(review, images, reports, actionHistories);
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

	@Transactional
	public void updateStatus(Long reviewId, ReviewStatus status) {
		Review review = reviewRepository.findById(reviewId)
			.orElseThrow(ReviewErrorCode.REVIEW_NOT_FOUND::exception);

		review.changeStatus(status);
	}

	public ReviewMaskPreviewResponse previewMask(Long reviewId) {
		Review review = reviewRepository.findById(reviewId)
			.orElseThrow(ReviewErrorCode.REVIEW_NOT_FOUND::exception);

		return ReviewMaskPreviewResponse.from(maskProhibitedWords(review.getContent()));
	}

	/**
	 * 마스킹을 확정한다. 확정 시점에 금칙어 사전으로 다시 매칭해 본문을 치환하고, 조치 사유를 조치 이력에 남긴다.
	 *
	 * <p>{@code adminId}는 {@code AdminSessionValidationFilter}가 이미 활성 관리자인지 검증한 뒤라
	 * 여기서는 FK로 쓸 참조만 얻는다.
	 */
	@Transactional
	public void mask(Long reviewId, Long adminId, List<ReviewMaskReason> reasons, String detailReason) {
		Review review = reviewRepository.findById(reviewId)
			.orElseThrow(ReviewErrorCode.REVIEW_NOT_FOUND::exception);

		review.mask(maskProhibitedWords(review.getContent()));

		Admin admin = adminRepository.getReferenceById(adminId);
		actionHistoryRepository.save(
			ActionHistory.forReview(review, admin, ReviewMaskReason.join(reasons), detailReason)
		);
	}

	/** 금칙어 글자 사이에 한글이 아닌 문자가 최대 2개까지 끼어도 우회로 보고 매칭한다. */
	private static final int MAX_INSERTED_CHARS_BETWEEN = 2;

	/**
	 * 활성 금칙어와 본문을 대조해 일치하는 부분을 매칭된 길이만큼 {@code *}로 치환한다.
	 *
	 * <p>{@code "씨1발"}처럼 금칙어 글자 사이에 숫자·특수문자·공백을 끼워 넣는 우회 표기까지 잡기 위해
	 * 단순 부분 문자열 비교 대신 글자 사이에 {@code [^가-힣]{0,2}}를 끼운 정규식으로 매칭한다.
	 */
	private String maskProhibitedWords(String content) {
		List<ProhibitedWord> enabledWords = prohibitedWordRepository.findAllByEnabledTrueAndDeletedAtIsNull();

		String masked = content;
		for (ProhibitedWord prohibitedWord : enabledWords) {
			Pattern pattern = buildLoosePattern(prohibitedWord.getWord());
			Matcher matcher = pattern.matcher(masked);
			masked = matcher.replaceAll(result -> "*".repeat(result.group().length()));
		}

		return masked;
	}

	/** {@code "욕설"} → {@code "욕[^가-힣]{0,2}설"}처럼 글자 사이에 우회 구분자를 허용하는 패턴을 만든다. */
	private Pattern buildLoosePattern(String word) {
		String loosePattern = word.chars()
			.mapToObj(character -> Pattern.quote(String.valueOf((char) character)))
			.collect(Collectors.joining("[^가-힣]{0," + MAX_INSERTED_CHARS_BETWEEN + "}"));

		return Pattern.compile(loosePattern);
	}
}
