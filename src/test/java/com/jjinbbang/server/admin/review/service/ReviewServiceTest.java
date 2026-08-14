package com.jjinbbang.server.admin.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.jjinbbang.server.admin.moderation.repository.ProhibitedWordFlagRepository;
import com.jjinbbang.server.admin.moderation.repository.ReportRepository;
import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewResponse;
import com.jjinbbang.server.domain.common.entity.University;
import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.domain.review.repository.ReviewRepository;
import com.jjinbbang.server.domain.review.type.ReviewStatus;
import com.jjinbbang.server.domain.user.entity.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService")
class ReviewServiceTest {

	private static final Pageable PAGEABLE = PageRequest.of(0, 20);

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private ProhibitedWordFlagRepository prohibitedWordFlagRepository;

	@InjectMocks
	private ReviewService reviewService;

	@Test
	@DisplayName("조회된 리뷰가 없으면 금칙어·신고 건수 배치 조회를 하지 않는다")
	void 리뷰가_없으면_배치_조회를_하지_않는다() {
		// given
		given(reviewRepository.findAll(ArgumentMatchers.<Specification<Review>>any(), eq(PAGEABLE)))
			.willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

		// when
		ReviewListResponse response = reviewService.findAll(null, null, null, null, false, PAGEABLE);

		// then
		assertThat(response.reviewList()).isEmpty();
		then(prohibitedWordFlagRepository).should(never()).findFlaggedReviewIds(anyList());
		then(reportRepository).should(never()).countByReviewIdIn(anyList());
	}

	@Test
	@DisplayName("금칙어 플래그와 신고 건수가 각 리뷰에 정확히 반영된다")
	void 금칙어_플래그와_신고_건수가_반영된다() {
		// given
		Review flagged = review(1L, "욕설 포함 리뷰");
		Review clean = review(2L, "평범한 리뷰");
		given(reviewRepository.findAll(ArgumentMatchers.<Specification<Review>>any(), eq(PAGEABLE)))
			.willReturn(new PageImpl<>(List.of(flagged, clean), PAGEABLE, 2));
		given(prohibitedWordFlagRepository.findFlaggedReviewIds(List.of(1L, 2L)))
			.willReturn(List.of(1L));
		given(reportRepository.countByReviewIdIn(List.of(1L, 2L)))
			.willReturn(List.<Object[]>of(new Object[] {2L, 3L}));

		// when
		List<ReviewResponse> reviewList = reviewService
			.findAll(null, null, null, null, false, PAGEABLE)
			.reviewList();

		// then
		ReviewResponse flaggedResponse = reviewList.get(0);
		assertThat(flaggedResponse.hasBadWord()).isTrue();
		assertThat(flaggedResponse.reportCount()).isZero();

		ReviewResponse cleanResponse = reviewList.get(1);
		assertThat(cleanResponse.hasBadWord()).isFalse();
		assertThat(cleanResponse.reportCount()).isEqualTo(3L);
	}

	@Test
	@DisplayName("리뷰에 작성자 학교와 닉네임을 채워서 응답한다")
	void 작성자_정보를_채워서_응답한다() {
		// given
		given(reviewRepository.findAll(ArgumentMatchers.<Specification<Review>>any(), eq(PAGEABLE)))
			.willReturn(new PageImpl<>(List.of(review(1L, "내용")), PAGEABLE, 1));
		given(prohibitedWordFlagRepository.findFlaggedReviewIds(List.of(1L))).willReturn(List.of());
		given(reportRepository.countByReviewIdIn(List.of(1L))).willReturn(List.of());

		// when
		ReviewResponse response = reviewService.findAll(null, null, null, null, false, PAGEABLE)
			.reviewList()
			.getFirst();

		// then
		assertThat(response.schoolName()).isEqualTo("찐빵대학교");
		assertThat(response.userName()).isEqualTo("익명의찐빵이");
		assertThat(response.reportCount()).isZero();
	}

	@Test
	@DisplayName("리뷰 조회는 항상 Specification 을 만들어 리포지토리에 위임한다")
	void 리뷰_조회는_Specification으로_위임한다() {
		// given
		given(reviewRepository.findAll(ArgumentMatchers.<Specification<Review>>any(), eq(PAGEABLE)))
			.willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

		// when
		reviewService.findAll("키워드", List.of("찐빵대학교"), null, ReviewStatus.PUBLIC, true, PAGEABLE);

		// then
		then(reviewRepository).should().findAll(ArgumentMatchers.<Specification<Review>>any(), eq(PAGEABLE));
	}

	private Review review(Long id, String content) {
		Review review = BeanUtils.instantiateClass(Review.class);
		ReflectionTestUtils.setField(review, "id", id);
		ReflectionTestUtils.setField(review, "user", author());
		ReflectionTestUtils.setField(review, "status", ReviewStatus.PUBLIC);
		ReflectionTestUtils.setField(review, "content", content);
		ReflectionTestUtils.setField(review, "rating", 5);
		ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.of(2026, 8, 1, 10, 0));
		return review;
	}

	private User author() {
		University university = BeanUtils.instantiateClass(University.class);
		ReflectionTestUtils.setField(university, "name", "찐빵대학교");

		User user = BeanUtils.instantiateClass(User.class);
		ReflectionTestUtils.setField(user, "id", 5L);
		ReflectionTestUtils.setField(user, "university", university);
		ReflectionTestUtils.setField(user, "nickname", "익명의찐빵이");
		return user;
	}
}
