package com.jjinbbang.server.admin.moderation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.jjinbbang.server.admin.moderation.dto.response.ReportDismissResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ReportListResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ReportResponse;
import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.exception.ModerationErrorCode;
import com.jjinbbang.server.admin.moderation.repository.ReportRepository;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;
import com.jjinbbang.server.domain.common.entity.University;
import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.domain.user.entity.User;
import com.jjinbbang.server.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService")
class ReportServiceTest {

	private static final Pageable PAGEABLE = PageRequest.of(0, 20);

	@Mock
	private ReportRepository reportRepository;

	@InjectMocks
	private ReportService reportService;

	@Test
	@DisplayName("상태 필터가 없으면 전체 신고를 조회한다")
	void 필터가_없으면_전부_조회한다() {
		// given
		given(reportRepository.findPageWithDetails(PAGEABLE))
			.willReturn(new PageImpl<>(List.of(report(1L, ReportStatus.PENDING, reporter())), PAGEABLE, 1));

		// when
		ReportListResponse response = reportService.findAll(null, PAGEABLE);

		// then
		assertThat(response.reportList()).hasSize(1);
		assertThat(response.pageInfo().totalElements()).isEqualTo(1);
		then(reportRepository).should(never()).findPageByStatusWithDetails(any(), any());
	}

	@Test
	@DisplayName("상태 필터가 있으면 해당 상태만 조회한다")
	void 필터가_있으면_해당_상태만_조회한다() {
		// given
		given(reportRepository.findPageByStatusWithDetails(ReportStatus.PENDING, PAGEABLE))
			.willReturn(new PageImpl<>(List.of(report(1L, ReportStatus.PENDING, reporter())), PAGEABLE, 1));

		// when
		reportService.findAll(ReportStatus.PENDING, PAGEABLE);

		// then
		then(reportRepository).should().findPageByStatusWithDetails(ReportStatus.PENDING, PAGEABLE);
		then(reportRepository).should(never()).findPageWithDetails(any());
	}

	@Test
	@DisplayName("리뷰 제목 컬럼이 없어 신고 목록은 리뷰 본문과 신고자 소속을 내려준다")
	void 목록은_리뷰_본문과_신고자_소속을_내려준다() {
		// given
		given(reportRepository.findPageWithDetails(PAGEABLE))
			.willReturn(new PageImpl<>(List.of(report(1L, ReportStatus.PENDING, reporter())), PAGEABLE, 1));

		// when
		ReportResponse response = reportService.findAll(null, PAGEABLE).reportList().getFirst();

		// then
		assertThat(response.reportId()).isEqualTo(1L);
		assertThat(response.reviewId()).isEqualTo(12L);
		assertThat(response.reviewContent()).isEqualTo("리뷰 본문");
		assertThat(response.reporterId()).isEqualTo(5L);
		assertThat(response.reporterUniversity()).isEqualTo("찐빵대학교");
		assertThat(response.reason()).isEqualTo("욕설/비방");
	}

	@Test
	@DisplayName("탈퇴 회원의 신고는 신고자 정보가 null 로 나간다")
	void 탈퇴_회원의_신고는_신고자가_null() {
		// given — reports.user_id 는 nullable 이다
		given(reportRepository.findPageWithDetails(PAGEABLE))
			.willReturn(new PageImpl<>(List.of(report(1L, ReportStatus.PENDING, null)), PAGEABLE, 1));

		// when
		ReportResponse response = reportService.findAll(null, PAGEABLE).reportList().getFirst();

		// then
		assertThat(response.reporterId()).isNull();
		assertThat(response.reporterUniversity()).isNull();
		assertThat(response.reviewContent()).isEqualTo("리뷰 본문");
	}

	@Test
	@DisplayName("대기 중인 신고를 기각하면 REJECT 가 된다")
	void 기각하면_REJECT_가_된다() {
		// given
		Report report = report(1L, ReportStatus.PENDING, reporter());
		given(reportRepository.findById(1L)).willReturn(Optional.of(report));

		// when
		ReportDismissResponse response = reportService.dismiss(1L);

		// then
		assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECT);
		assertThat(response.status()).isEqualTo(ReportStatus.REJECT);
		assertThat(response.reportId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("없는 신고를 기각하면 404")
	void 없는_신고_기각은_404() {
		// given
		given(reportRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> reportService.dismiss(999L))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ModerationErrorCode.REPORT_NOT_FOUND);
	}

	@ParameterizedTest
	@EnumSource(value = ReportStatus.class, names = {"APPROVE", "REJECT"})
	@DisplayName("이미 처리된 신고를 다시 기각하면 409 이고 상태가 덮어써지지 않는다")
	void 이미_처리된_신고는_409(ReportStatus handled) {
		// given
		Report report = report(1L, handled, reporter());
		given(reportRepository.findById(1L)).willReturn(Optional.of(report));

		// when & then
		assertThatThrownBy(() -> reportService.dismiss(1L))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ModerationErrorCode.REPORT_ALREADY_HANDLED);

		assertThat(report.getStatus()).isEqualTo(handled);
	}

	private Report report(Long id, ReportStatus status, User reporter) {
		Review review = BeanUtils.instantiateClass(Review.class);
		ReflectionTestUtils.setField(review, "id", 12L);
		ReflectionTestUtils.setField(review, "content", "리뷰 본문");

		Report report = BeanUtils.instantiateClass(Report.class);
		ReflectionTestUtils.setField(report, "id", id);
		ReflectionTestUtils.setField(report, "review", review);
		ReflectionTestUtils.setField(report, "user", reporter);
		ReflectionTestUtils.setField(report, "reason", "욕설/비방");
		ReflectionTestUtils.setField(report, "status", status);
		return report;
	}

	private User reporter() {
		University university = BeanUtils.instantiateClass(University.class);
		ReflectionTestUtils.setField(university, "name", "찐빵대학교");

		User user = BeanUtils.instantiateClass(User.class);
		ReflectionTestUtils.setField(user, "id", 5L);
		ReflectionTestUtils.setField(user, "university", university);
		return user;
	}
}
