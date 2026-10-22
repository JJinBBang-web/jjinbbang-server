package com.jjinbbang.server.admin.moderation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.jjinbbang.server.admin.moderation.dto.response.ReportDismissResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ReportReviewListResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ReportReviewResponse;
import com.jjinbbang.server.admin.moderation.dto.response.SubReportResponse;
import com.jjinbbang.server.admin.moderation.exception.ModerationErrorCode;
import com.jjinbbang.server.admin.moderation.service.ReportService;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;
import com.jjinbbang.server.global.error.GlobalExceptionHandler;
import com.jjinbbang.server.global.paging.PageInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportController")
class ReportControllerTest {

	@Mock
	private ReportService reportService;

	@InjectMocks
	private ReportController reportController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(reportController)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("목록 조회는 리뷰 단위 reportReviewList 와 pageInfo 로 나가고, 신고는 subReports 에 담긴다")
	void 목록_응답_형태() throws Exception {
		// given
		given(reportService.findAll(any(), any())).willReturn(listResponse());

		// when & then
		mockMvc.perform(get("/api/admin/reports"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("신고 목록 조회 성공"))
			.andExpect(jsonPath("$.data.reportReviewList[0].reviewId").value(12))
			.andExpect(jsonPath("$.data.reportReviewList[0].schoolName").value("부산대학교"))
			.andExpect(jsonPath("$.data.reportReviewList[0].content").value("리뷰 본문"))
			.andExpect(jsonPath("$.data.reportReviewList[0].totalReportCount").value(1))
			.andExpect(jsonPath("$.data.reportReviewList[0].subReports[0].reportId").value(1))
			.andExpect(jsonPath("$.data.reportReviewList[0].subReports[0].reporterId").value(5))
			.andExpect(jsonPath("$.data.reportReviewList[0].subReports[0].reason").value("욕설/비방"))
			.andExpect(jsonPath("$.data.reportReviewList[0].subReports[0].status").value("PENDING"))
			.andExpect(jsonPath("$.data.reportReviewList[0].subReports[0].reportedAt").value("2026-08-01T10:00:00"))
			.andExpect(jsonPath("$.data.pageInfo.totalElements").value(1));
	}

	@Test
	@DisplayName("예전 신고 단위 필드는 응답에 남지 않는다")
	void 예전_신고_단위_필드는_없다() throws Exception {
		// given
		given(reportService.findAll(any(), any())).willReturn(listResponse());

		// when & then
		mockMvc.perform(get("/api/admin/reports"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.reportList").doesNotExist())
			.andExpect(jsonPath("$.data.reportReviewList[0].subReports[0].reviewId").doesNotExist())
			.andExpect(jsonPath("$.data.reportReviewList[0].subReports[0].createdAt").doesNotExist());
	}

	@Test
	@DisplayName("status 를 주지 않으면 필터 없이 조회한다")
	void status_가_없으면_전체_조회() throws Exception {
		// given
		given(reportService.findAll(any(), any())).willReturn(listResponse());

		// when
		mockMvc.perform(get("/api/admin/reports")).andExpect(status().isOk());

		// then
		then(reportService).should().findAll(isNull(), any());
	}

	@Test
	@DisplayName("status 를 주면 enum 으로 변환돼 전달된다")
	void status_가_enum_으로_전달된다() throws Exception {
		// given
		given(reportService.findAll(any(), any())).willReturn(listResponse());

		// when
		mockMvc.perform(get("/api/admin/reports").param("status", "PENDING"))
			.andExpect(status().isOk());

		// then
		then(reportService).should().findAll(eq(ReportStatus.PENDING), any());
	}

	@Test
	@DisplayName("status 가 enum 밖의 값이면 400 INVALID_TYPE")
	void 알_수_없는_status_는_400() throws Exception {
		mockMvc.perform(get("/api/admin/reports").param("status", "DONE"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_TYPE"))
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다. (status)"));
	}

	@Test
	@DisplayName("기각에 성공하면 200 이고 바뀐 상태를 돌려준다")
	void 기각_성공은_200() throws Exception {
		// given
		given(reportService.dismiss(1L)).willReturn(new ReportDismissResponse(1L, ReportStatus.REJECT));

		// when & then
		mockMvc.perform(post("/api/admin/reports/dismiss/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("신고 기각 성공"))
			.andExpect(jsonPath("$.data.reportId").value(1))
			.andExpect(jsonPath("$.data.status").value("REJECT"));
	}

	@Test
	@DisplayName("없는 신고를 기각하면 404")
	void 없는_신고_기각은_404() throws Exception {
		// given
		given(reportService.dismiss(999L)).willThrow(ModerationErrorCode.REPORT_NOT_FOUND.exception());

		// when & then
		mockMvc.perform(post("/api/admin/reports/dismiss/999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("REPORT_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("해당 신고 정보가 존재하지 않습니다."));
	}

	@Test
	@DisplayName("이미 처리된 신고를 기각하면 409")
	void 이미_처리된_신고_기각은_409() throws Exception {
		// given
		given(reportService.dismiss(1L)).willThrow(ModerationErrorCode.REPORT_ALREADY_HANDLED.exception());

		// when & then
		mockMvc.perform(post("/api/admin/reports/dismiss/1"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("REPORT_ALREADY_HANDLED"));
	}

	@Test
	@DisplayName("reportId 가 숫자가 아니면 400 INVALID_TYPE")
	void reportId_타입_불일치는_400() throws Exception {
		mockMvc.perform(post("/api/admin/reports/dismiss/abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_TYPE"))
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다. (reportId)"));
	}

	private ReportReviewListResponse listResponse() {
		SubReportResponse subReport = new SubReportResponse(
			1L, 5L, "욕설/비방", ReportStatus.PENDING, LocalDateTime.of(2026, 8, 1, 10, 0)
		);
		return new ReportReviewListResponse(
			List.of(new ReportReviewResponse(12L, "부산대학교", "리뷰 본문", 1, List.of(subReport))),
			new PageInfo(0, 20, 1, 1)
		);
	}
}
