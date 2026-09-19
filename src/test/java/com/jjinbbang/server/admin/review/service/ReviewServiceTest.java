package com.jjinbbang.server.admin.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

import com.jjinbbang.server.admin.administrator.entity.ActionHistory;
import com.jjinbbang.server.admin.administrator.entity.Admin;
import com.jjinbbang.server.admin.administrator.repository.ActionHistoryRepository;
import com.jjinbbang.server.admin.administrator.repository.AdminRepository;
import com.jjinbbang.server.admin.moderation.entity.ProhibitedWord;
import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.repository.ProhibitedWordRepository;
import com.jjinbbang.server.admin.moderation.repository.ReportRepository;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;
import com.jjinbbang.server.admin.review.dto.response.ReviewDetailResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewMaskPreviewResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewResponse;
import com.jjinbbang.server.admin.review.type.ReviewMaskReason;
import com.jjinbbang.server.domain.common.entity.University;
import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.domain.review.entity.ReviewImage;
import com.jjinbbang.server.domain.review.exception.ReviewErrorCode;
import com.jjinbbang.server.domain.review.repository.ReviewImageRepository;
import com.jjinbbang.server.domain.review.repository.ReviewRepository;
import com.jjinbbang.server.domain.review.type.ReviewStatus;
import com.jjinbbang.server.domain.user.entity.User;
import com.jjinbbang.server.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService")
class ReviewServiceTest {

	private static final Pageable PAGEABLE = PageRequest.of(0, 20);

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private ReviewImageRepository reviewImageRepository;

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private ProhibitedWordRepository prohibitedWordRepository;

	@Mock
	private ActionHistoryRepository actionHistoryRepository;

	@Mock
	private AdminRepository adminRepository;

	@InjectMocks
	private ReviewService reviewService;

	@Test
	@DisplayName("조회된 리뷰가 없으면 신고 건수 배치 조회를 하지 않는다")
	void 리뷰가_없으면_배치_조회를_하지_않는다() {
		// given
		given(reviewRepository.findAll(ArgumentMatchers.<Specification<Review>>any(), eq(PAGEABLE)))
			.willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

		// when
		ReviewListResponse response = reviewService.findAll(null, null, null, null, false, PAGEABLE);

		// then
		assertThat(response.reviewList()).isEmpty();
		then(reportRepository).should(never()).countByReviewIdIn(anyList());
	}

	@Test
	@DisplayName("금칙어 플래그와 신고 건수가 각 리뷰에 정확히 반영된다")
	void 금칙어_플래그와_신고_건수가_반영된다() {
		// given
		Review flagged = review(1L, "욕설 포함 리뷰");
		ReflectionTestUtils.setField(flagged, "prohibitedWordFlag", true);
		Review clean = review(2L, "평범한 리뷰");
		given(reviewRepository.findAll(ArgumentMatchers.<Specification<Review>>any(), eq(PAGEABLE)))
			.willReturn(new PageImpl<>(List.of(flagged, clean), PAGEABLE, 2));
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

	@Test
	@DisplayName("존재하지 않는 리뷰를 상세 조회하면 404를 던진다")
	void 존재하지_않는_리뷰는_404() {
		// given
		given(reviewRepository.findDetailById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> reviewService.findById(999L))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
	}

	@Test
	@DisplayName("리뷰 상세는 작성자·학교·이미지·신고·조치 이력을 채워 응답한다")
	void 리뷰_상세는_연관_정보를_채워_응답한다() {
		// given
		Review review = review(1L, "욕설 포함 리뷰", "kim1234@pusan.ac.kr");
		ReflectionTestUtils.setField(review, "prohibitedWordFlag", true);
		given(reviewRepository.findDetailById(1L)).willReturn(Optional.of(review));
		given(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(1L))
			.willReturn(List.of(reviewImage(review, "https://img/1"), reviewImage(review, "https://img/2")));
		given(reportRepository.findByReviewIdOrderByCreatedAtAsc(1L))
			.willReturn(List.of(report(review, author(), "욕설·비방")));
		given(actionHistoryRepository.findByReviewIdOrderByCreatedAtAsc(1L))
			.willReturn(List.of(actionHistory("신고 기각", "근거 부족", "ddochi")));

		// when
		ReviewDetailResponse response = reviewService.findById(1L);

		// then
		assertThat(response.reviewId()).isEqualTo(1L);
		assertThat(response.schoolName()).isEqualTo("찐빵대학교");
		assertThat(response.nickname()).isEqualTo("익명의찐빵이");
		assertThat(response.email()).isEqualTo("kim****@pusan.ac.kr");
		assertThat(response.hasBadWordFlag()).isTrue();
		assertThat(response.images()).containsExactly("https://img/1", "https://img/2");
		assertThat(response.reportCount()).isEqualTo(1L);
		assertThat(response.reportList()).hasSize(1);
		assertThat(response.reportList().getFirst().reportReason()).isEqualTo("욕설·비방");
		assertThat(response.reportList().getFirst().reporterId()).isEqualTo(5L);
		assertThat(response.historyList()).hasSize(1);
		assertThat(response.historyList().getFirst().actionNames()).containsExactly("신고 기각");
		assertThat(response.historyList().getFirst().reason()).isEqualTo("근거 부족");
		assertThat(response.historyList().getFirst().handler()).isEqualTo("ddochi");
	}

	@Test
	@DisplayName("여러 사유가 쉼표로 합쳐진 조치 이력은 개별 사유 리스트로 쪼개져 응답한다")
	void 조치_이력의_여러_사유가_리스트로_쪼개진다() {
		// given
		Review review = review(1L, "내용");
		given(reviewRepository.findDetailById(1L)).willReturn(Optional.of(review));
		given(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(1L)).willReturn(List.of());
		given(reportRepository.findByReviewIdOrderByCreatedAtAsc(1L)).willReturn(List.of());
		given(actionHistoryRepository.findByReviewIdOrderByCreatedAtAsc(1L))
			.willReturn(List.of(actionHistory("BAD_WORD,PRIVACY_EXPOSURE", "직접 입력한 사유", "ddochi")));

		// when
		ReviewDetailResponse response = reviewService.findById(1L);

		// then
		assertThat(response.historyList().getFirst().actionNames())
			.containsExactly("BAD_WORD", "PRIVACY_EXPOSURE");
	}

	@Test
	@DisplayName("학교 이메일이 없으면 마스킹 없이 null 로 나간다")
	void 이메일이_없으면_null() {
		// given
		Review review = review(1L, "내용", null);
		given(reviewRepository.findDetailById(1L)).willReturn(Optional.of(review));
		given(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(1L)).willReturn(List.of());
		given(reportRepository.findByReviewIdOrderByCreatedAtAsc(1L)).willReturn(List.of());
		given(actionHistoryRepository.findByReviewIdOrderByCreatedAtAsc(1L)).willReturn(List.of());

		// when
		ReviewDetailResponse response = reviewService.findById(1L);

		// then
		assertThat(response.email()).isNull();
	}

	@Test
	@DisplayName("존재하지 않는 리뷰를 삭제하면 404를 던진다")
	void 존재하지_않는_리뷰_삭제는_404() {
		// given
		given(reviewRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> reviewService.delete(999L))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
	}

	@Test
	@DisplayName("이미 삭제된 리뷰를 다시 삭제하면 409를 던지고 삭제 시각이 덮어써지지 않는다")
	void 이미_삭제된_리뷰_삭제는_409() {
		// given
		Review review = review(1L, "내용");
		LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 1, 9, 0);
		ReflectionTestUtils.setField(review, "deletedAt", deletedAt);
		given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

		// when & then
		assertThatThrownBy(() -> reviewService.delete(1L))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ReviewErrorCode.REVIEW_ALREADY_DELETED);

		assertThat(review.getDeletedAt()).isEqualTo(deletedAt);
	}

	@Test
	@DisplayName("리뷰를 삭제하면 소프트 삭제된다")
	void 리뷰를_삭제하면_소프트_삭제된다() {
		// given
		Review review = review(1L, "내용");
		given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

		// when
		reviewService.delete(1L);

		// then
		assertThat(review.isDeleted()).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 리뷰는 상태를 변경하면 404를 던진다")
	void 존재하지_않는_리뷰_상태_변경은_404() {
		// given
		given(reviewRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> reviewService.updateStatus(999L, ReviewStatus.PRIVATE))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
	}

	@Test
	@DisplayName("리뷰 상태를 변경한다")
	void 리뷰_상태를_변경한다() {
		// given
		Review review = review(1L, "내용");
		given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

		// when
		reviewService.updateStatus(1L, ReviewStatus.PRIVATE);

		// then
		assertThat(review.getStatus()).isEqualTo(ReviewStatus.PRIVATE);
	}

	@Test
	@DisplayName("존재하지 않는 리뷰는 마스킹 미리보기를 조회하면 404를 던진다")
	void 존재하지_않는_리뷰_마스킹_미리보기는_404() {
		// given
		given(reviewRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> reviewService.previewMask(999L))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
	}

	@Test
	@DisplayName("활성 금칙어와 일치하는 부분만 같은 길이의 * 로 치환한다")
	void 활성_금칙어를_마스킹한다() {
		// given
		Review review = review(1L, "집주인이 욕설 진짜 별로예요");
		given(reviewRepository.findById(1L)).willReturn(Optional.of(review));
		given(prohibitedWordRepository.findAllByEnabledTrueAndDeletedAtIsNull())
			.willReturn(List.of(prohibitedWord("욕설")));

		// when
		ReviewMaskPreviewResponse response = reviewService.previewMask(1L);

		// then
		assertThat(response.maskedContent()).isEqualTo("집주인이 ** 진짜 별로예요");
	}

	@Test
	@DisplayName("비활성 금칙어는 조회 대상에서 빠지므로 마스킹되지 않는다")
	void 비활성_금칙어는_조회되지_않아_마스킹되지_않는다() {
		// given
		Review review = review(1L, "집주인이 욕설 진짜 별로예요");
		given(reviewRepository.findById(1L)).willReturn(Optional.of(review));
		given(prohibitedWordRepository.findAllByEnabledTrueAndDeletedAtIsNull()).willReturn(List.of());

		// when
		ReviewMaskPreviewResponse response = reviewService.previewMask(1L);

		// then
		assertThat(response.maskedContent()).isEqualTo("집주인이 욕설 진짜 별로예요");
	}

	@Test
	@DisplayName("금칙어 글자 사이에 문자가 2개까지 끼어도 우회로 보고 매칭 길이만큼 마스킹한다")
	void 글자_사이에_문자가_2개까지_끼어도_마스킹된다() {
		// given
		Review review = review(1L, "집주인이 씨1@발 진짜 별로예요");
		given(reviewRepository.findById(1L)).willReturn(Optional.of(review));
		given(prohibitedWordRepository.findAllByEnabledTrueAndDeletedAtIsNull())
			.willReturn(List.of(prohibitedWord("씨발")));

		// when
		ReviewMaskPreviewResponse response = reviewService.previewMask(1L);

		// then
		assertThat(response.maskedContent()).isEqualTo("집주인이 **** 진짜 별로예요");
	}

	@Test
	@DisplayName("금칙어 글자 사이에 문자가 3개 이상 끼면 우회로 보지 않고 마스킹하지 않는다")
	void 글자_사이에_문자가_3개_이상이면_마스킹되지_않는다() {
		// given
		Review review = review(1L, "집주인이 씨123발 진짜 별로예요");
		given(reviewRepository.findById(1L)).willReturn(Optional.of(review));
		given(prohibitedWordRepository.findAllByEnabledTrueAndDeletedAtIsNull())
			.willReturn(List.of(prohibitedWord("씨발")));

		// when
		ReviewMaskPreviewResponse response = reviewService.previewMask(1L);

		// then
		assertThat(response.maskedContent()).isEqualTo("집주인이 씨123발 진짜 별로예요");
	}

	@Test
	@DisplayName("존재하지 않는 리뷰는 마스킹을 확정하면 404를 던진다")
	void 존재하지_않는_리뷰_마스킹_확정은_404() {
		// given
		given(reviewRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> reviewService.mask(999L, 1L, List.of(ReviewMaskReason.BAD_WORD), null))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);

		then(actionHistoryRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("마스킹을 확정하면 본문이 치환되고 금칙어 플래그가 세워지며 조치 이력이 남는다")
	void 마스킹을_확정한다() {
		// given
		Review review = review(1L, "집주인이 욕설 진짜 별로예요");
		Admin admin = BeanUtils.instantiateClass(Admin.class);
		given(reviewRepository.findById(1L)).willReturn(Optional.of(review));
		given(prohibitedWordRepository.findAllByEnabledTrueAndDeletedAtIsNull())
			.willReturn(List.of(prohibitedWord("욕설")));
		given(adminRepository.getReferenceById(9L)).willReturn(admin);

		// when
		reviewService.mask(1L, 9L, List.of(ReviewMaskReason.BAD_WORD), "직접 입력한 사유");

		// then
		assertThat(review.getContent()).isEqualTo("집주인이 ** 진짜 별로예요");
		assertThat(review.isProhibitedWordFlag()).isTrue();

		then(actionHistoryRepository).should().save(argThat(actionHistory ->
			actionHistory.getReview() == review
				&& actionHistory.getAdmin() == admin
				&& actionHistory.getReason().equals("BAD_WORD")
				&& actionHistory.getDetailReason().equals("직접 입력한 사유")
		));
	}

	@Test
	@DisplayName("사유를 여러 개 선택하면 쉼표로 이어붙여 조치 이력에 남는다")
	void 여러_사유를_선택하면_쉼표로_합쳐_저장한다() {
		// given
		Review review = review(1L, "내용");
		Admin admin = BeanUtils.instantiateClass(Admin.class);
		given(reviewRepository.findById(1L)).willReturn(Optional.of(review));
		given(prohibitedWordRepository.findAllByEnabledTrueAndDeletedAtIsNull()).willReturn(List.of());
		given(adminRepository.getReferenceById(9L)).willReturn(admin);

		// when
		reviewService.mask(1L, 9L, List.of(ReviewMaskReason.BAD_WORD, ReviewMaskReason.PRIVACY_EXPOSURE), null);

		// then
		then(actionHistoryRepository).should().save(argThat(actionHistory ->
			actionHistory.getReason().equals("BAD_WORD,PRIVACY_EXPOSURE")
		));
	}

	private ProhibitedWord prohibitedWord(String word) {
		ProhibitedWord prohibitedWord = BeanUtils.instantiateClass(ProhibitedWord.class);
		ReflectionTestUtils.setField(prohibitedWord, "word", word);
		ReflectionTestUtils.setField(prohibitedWord, "enabled", true);
		return prohibitedWord;
	}

	private ReviewImage reviewImage(Review review, String url) {
		ReviewImage image = BeanUtils.instantiateClass(ReviewImage.class);
		ReflectionTestUtils.setField(image, "review", review);
		ReflectionTestUtils.setField(image, "url", url);
		return image;
	}

	private Report report(Review review, User reporter, String reason) {
		Report report = BeanUtils.instantiateClass(Report.class);
		ReflectionTestUtils.setField(report, "review", review);
		ReflectionTestUtils.setField(report, "user", reporter);
		ReflectionTestUtils.setField(report, "reason", reason);
		ReflectionTestUtils.setField(report, "status", ReportStatus.PENDING);
		ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 6, 2, 14, 30));
		return report;
	}

	private ActionHistory actionHistory(String reason, String detailReason, String adminUsername) {
		Admin admin = BeanUtils.instantiateClass(Admin.class);
		ReflectionTestUtils.setField(admin, "username", adminUsername);

		ActionHistory actionHistory = BeanUtils.instantiateClass(ActionHistory.class);
		ReflectionTestUtils.setField(actionHistory, "admin", admin);
		ReflectionTestUtils.setField(actionHistory, "reason", reason);
		ReflectionTestUtils.setField(actionHistory, "detailReason", detailReason);
		ReflectionTestUtils.setField(actionHistory, "createdAt", LocalDateTime.of(2026, 6, 2, 14, 2));
		return actionHistory;
	}

	private Review review(Long id, String content, String universityEmail) {
		Review review = review(id, content);
		ReflectionTestUtils.setField(review.getUser(), "universityEmail", universityEmail);
		return review;
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
