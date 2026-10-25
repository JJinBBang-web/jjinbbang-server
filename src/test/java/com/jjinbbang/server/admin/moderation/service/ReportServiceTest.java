package com.jjinbbang.server.admin.moderation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
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
import com.jjinbbang.server.admin.moderation.dto.response.ReportReviewListResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ReportReviewResponse;
import com.jjinbbang.server.admin.moderation.dto.response.SubReportResponse;
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
	private static final LocalDateTime REPORTED_AT = LocalDateTime.of(2026, 6, 2, 14, 30);

	@Mock
	private ReportRepository reportRepository;

	@InjectMocks
	private ReportService reportService;

	@Test
	@DisplayName("상태 필터가 없으면 상태를 가리지 않고 리뷰를 고르고 신고를 모은다")
	void 필터가_없으면_전부_조회한다() {
		// given
		given(reportRepository.findReportedReviewIds(PAGEABLE))
			.willReturn(new PageImpl<>(List.of(12L), PAGEABLE, 1));
		given(reportRepository.findAllWithReviewByReviewIdIn(List.of(12L)))
			.willReturn(List.of(report(1L, 12L, ReportStatus.PENDING, reporter())));

		// when
		ReportReviewListResponse response = reportService.findAll(null, PAGEABLE);

		// then
		assertThat(response.reportReviewList()).hasSize(1);
		assertThat(response.pageInfo().totalElements()).isEqualTo(1);
		then(reportRepository).should(never()).findReportedReviewIdsByStatus(any(), any());
		then(reportRepository).should(never()).findAllWithReviewByReviewIdInAndStatus(any(), any());
	}

	@Test
	@DisplayName("상태 필터가 있으면 리뷰 선별과 신고 모으기 양쪽에 같은 상태를 건다")
	void 필터가_있으면_해당_상태만_조회한다() {
		// given
		given(reportRepository.findReportedReviewIdsByStatus(ReportStatus.PENDING, PAGEABLE))
			.willReturn(new PageImpl<>(List.of(12L), PAGEABLE, 1));
		given(reportRepository.findAllWithReviewByReviewIdInAndStatus(List.of(12L), ReportStatus.PENDING))
			.willReturn(List.of(report(1L, 12L, ReportStatus.PENDING, reporter())));

		// when
		reportService.findAll(ReportStatus.PENDING, PAGEABLE);

		// then
		then(reportRepository).should(never()).findReportedReviewIds(any());
		then(reportRepository).should(never()).findAllWithReviewByReviewIdIn(any());
	}

	@Test
	@DisplayName("같은 리뷰의 신고는 한 행으로 묶이고, 리뷰 순서는 페이지 쿼리 순서를 따른다")
	void 같은_리뷰의_신고는_한_행으로_묶인다() {
		// given — 페이지 쿼리는 최근 신고일 순(13 → 12)이고, 신고 쿼리는 오래된 순이다
		given(reportRepository.findReportedReviewIds(PAGEABLE))
			.willReturn(new PageImpl<>(List.of(13L, 12L), PAGEABLE, 2));
		given(reportRepository.findAllWithReviewByReviewIdIn(List.of(13L, 12L)))
			.willReturn(List.of(
				report(1L, 12L, ReportStatus.PENDING, reporter()),
				report(2L, 13L, ReportStatus.PENDING, reporter()),
				report(3L, 12L, ReportStatus.PENDING, null)
			));

		// when
		List<ReportReviewResponse> rows = reportService.findAll(null, PAGEABLE).reportReviewList();

		// then
		assertThat(rows).extracting(ReportReviewResponse::reviewId).containsExactly(13L, 12L);
		assertThat(rows.get(1).totalReportCount()).isEqualTo(2);
		assertThat(rows.get(1).subReports()).extracting(SubReportResponse::reportId).containsExactly(1L, 3L);
	}

	@Test
	@DisplayName("리뷰 행에는 본문과 작성자 학교, 하위 신고에는 신고자 ID·사유·상태·신고일이 나간다")
	void 리뷰_행과_하위_신고의_필드() {
		// given
		given(reportRepository.findReportedReviewIds(PAGEABLE))
			.willReturn(new PageImpl<>(List.of(12L), PAGEABLE, 1));
		given(reportRepository.findAllWithReviewByReviewIdIn(List.of(12L)))
			.willReturn(List.of(report(1L, 12L, ReportStatus.PENDING, reporter())));

		// when
		ReportReviewResponse row = reportService.findAll(null, PAGEABLE).reportReviewList().getFirst();

		// then
		assertThat(row.reviewId()).isEqualTo(12L);
		assertThat(row.schoolName()).isEqualTo("부산대학교");
		assertThat(row.content()).isEqualTo("리뷰 본문");

		SubReportResponse subReport = row.subReports().getFirst();
		assertThat(subReport.reportId()).isEqualTo(1L);
		assertThat(subReport.reporterId()).isEqualTo(5L);
		assertThat(subReport.reason()).isEqualTo("욕설/비방");
		assertThat(subReport.status()).isEqualTo(ReportStatus.PENDING);
		assertThat(subReport.reportedAt()).isEqualTo(REPORTED_AT);
	}

	@Test
	@DisplayName("신고자가 없는 신고는 reporterId 가 null 로 나간다")
	void 신고자가_없으면_reporterId_가_null() {
		// given — reports.user_id 는 nullable 이다
		given(reportRepository.findReportedReviewIds(PAGEABLE))
			.willReturn(new PageImpl<>(List.of(12L), PAGEABLE, 1));
		given(reportRepository.findAllWithReviewByReviewIdIn(List.of(12L)))
			.willReturn(List.of(report(1L, 12L, ReportStatus.PENDING, null)));

		// when
		SubReportResponse subReport = reportService.findAll(null, PAGEABLE)
			.reportReviewList().getFirst().subReports().getFirst();

		// then
		assertThat(subReport.reporterId()).isNull();
	}

	@Test
	@DisplayName("두 쿼리 사이에 신고가 모두 처리돼 빈 리뷰가 생기면 그 행은 빼고 내려준다")
	void 신고가_사라진_리뷰는_빠진다() {
		// given — 12번 리뷰를 고른 직후 다른 관리자가 그 신고를 기각했다
		given(reportRepository.findReportedReviewIdsByStatus(ReportStatus.PENDING, PAGEABLE))
			.willReturn(new PageImpl<>(List.of(12L, 13L), PAGEABLE, 2));
		given(reportRepository.findAllWithReviewByReviewIdInAndStatus(List.of(12L, 13L), ReportStatus.PENDING))
			.willReturn(List.of(report(1L, 13L, ReportStatus.PENDING, reporter())));

		// when
		List<ReportReviewResponse> rows = reportService.findAll(ReportStatus.PENDING, PAGEABLE).reportReviewList();

		// then
		assertThat(rows).extracting(ReportReviewResponse::reviewId).containsExactly(13L);
	}

	@Test
	@DisplayName("신고된 리뷰가 없으면 신고 쿼리를 날리지 않고 빈 목록을 내려준다")
	void 빈_페이지면_신고를_조회하지_않는다() {
		// given
		given(reportRepository.findReportedReviewIds(PAGEABLE))
			.willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

		// when
		ReportReviewListResponse response = reportService.findAll(null, PAGEABLE);

		// then
		assertThat(response.reportReviewList()).isEmpty();
		assertThat(response.pageInfo().totalElements()).isZero();
		then(reportRepository).should(never()).findAllWithReviewByReviewIdIn(any());
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
		return report(id, 12L, status, reporter);
	}

	private Report report(Long id, Long reviewId, ReportStatus status, User reporter) {
		University university = BeanUtils.instantiateClass(University.class);
		ReflectionTestUtils.setField(university, "name", "부산대학교");

		User writer = BeanUtils.instantiateClass(User.class);
		ReflectionTestUtils.setField(writer, "id", 7L);
		ReflectionTestUtils.setField(writer, "university", university);

		Review review = BeanUtils.instantiateClass(Review.class);
		ReflectionTestUtils.setField(review, "id", reviewId);
		ReflectionTestUtils.setField(review, "user", writer);
		ReflectionTestUtils.setField(review, "originalContent", "리뷰 본문");

		Report report = BeanUtils.instantiateClass(Report.class);
		ReflectionTestUtils.setField(report, "id", id);
		ReflectionTestUtils.setField(report, "review", review);
		ReflectionTestUtils.setField(report, "user", reporter);
		ReflectionTestUtils.setField(report, "reason", "욕설/비방");
		ReflectionTestUtils.setField(report, "status", status);
		ReflectionTestUtils.setField(report, "createdAt", REPORTED_AT);
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
