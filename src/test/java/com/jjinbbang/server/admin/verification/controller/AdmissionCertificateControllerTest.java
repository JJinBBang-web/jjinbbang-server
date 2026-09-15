package com.jjinbbang.server.admin.verification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.jjinbbang.server.admin.administrator.security.AdminOidcUser;
import com.jjinbbang.server.admin.verification.dto.request.AdmissionCertificateRejectRequest;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse.CertificateSummary;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse.PageInfo;
import com.jjinbbang.server.admin.verification.service.AdmissionCertificateService;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;
import com.jjinbbang.server.global.error.GlobalExceptionHandler;
import com.jjinbbang.server.global.paging.PageRequests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("합격증명서 목록 조회 API")
class AdmissionCertificateControllerTest {

	AdmissionCertificateService admissionCertificateService;
	AdmissionCertificateController controller;
	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		admissionCertificateService = org.mockito.Mockito.mock(AdmissionCertificateService.class);
		controller = new AdmissionCertificateController(admissionCertificateService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("status, page, size로 목록을 조회한다")
	void getAdmissionCertificateList() throws Exception {
		AdmissionCertificateListResponse response = new AdmissionCertificateListResponse(
			List.of(new CertificateSummary(
				3001L,
				LocalDateTime.of(2026, 6, 5, 5, 10),
				10021L,
				true,
				"전남대"
			)),
			new PageInfo(0, 10, 1, 1, true, true)
		);
		when(admissionCertificateService.getAdmissionCertificateList(
			eq(AdmissionCertificateStatus.PENDING),
			any(Pageable.class)
		)).thenReturn(response);

		mockMvc.perform(get("/api/admin/certificates/admission")
				.queryParam("status", "PENDING")
				.queryParam("page", "0")
				.queryParam("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("증명서 목록 조회 성공"))
			.andExpect(jsonPath("$.data.certificateList[0].certificateId").value(3001))
			.andExpect(jsonPath("$.data.certificateList[0].createdAt").value("2026-06-05 05:10"))
			.andExpect(jsonPath("$.data.certificateList[0].userId").value(10021))
			.andExpect(jsonPath("$.data.certificateList[0].isReuploaded").value(true))
			.andExpect(jsonPath("$.data.certificateList[0].schoolName").value("전남대"))
			.andExpect(jsonPath("$.data.certificateList[0].userName").doesNotExist())
			.andExpect(jsonPath("$.data.pageInfo.currentPage").value(0))
			.andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))
			.andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
			.andExpect(jsonPath("$.data.pageInfo.totalPages").value(1))
			.andExpect(jsonPath("$.data.pageInfo.isFirst").value(true))
			.andExpect(jsonPath("$.data.pageInfo.isLast").value(true));

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(admissionCertificateService).getAdmissionCertificateList(
			eq(AdmissionCertificateStatus.PENDING),
			pageableCaptor.capture()
		);
		Pageable pageable = pageableCaptor.getValue();
		assertThat(pageable.getPageNumber()).isZero();
		assertThat(pageable.getPageSize()).isEqualTo(10);
		assertThat(pageable.getSort().getOrderFor("createdAt").isDescending())
			.isTrue();
		assertThat(pageable.getSort().getOrderFor("id").isDescending())
			.isTrue();
	}

	@Test
	@DisplayName("page와 size를 생략하면 기본값을 사용한다")
	void defaultPagination() throws Exception {
		AdmissionCertificateListResponse response = new AdmissionCertificateListResponse(
			List.of(),
			new PageInfo(0, PageRequests.DEFAULT_SIZE, 0, 0, true, true)
		);
		when(admissionCertificateService.getAdmissionCertificateList(
			eq(AdmissionCertificateStatus.APPROVE),
			any(Pageable.class)
		)).thenReturn(response);

		mockMvc.perform(get("/api/admin/certificates/admission")
				.queryParam("status", "APPROVE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.certificateList").isEmpty());

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(admissionCertificateService).getAdmissionCertificateList(
			eq(AdmissionCertificateStatus.APPROVE),
			pageableCaptor.capture()
		);
		assertThat(pageableCaptor.getValue().getPageSize())
			.isEqualTo(PageRequests.DEFAULT_SIZE);
	}

	@Test
	@DisplayName("status가 없으면 400 INVALID_INPUT을 반환한다")
	void statusIsRequired() throws Exception {
		mockMvc.perform(get("/api/admin/certificates/admission"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
	}

	@Test
	@DisplayName("음수 page와 상한을 넘는 size를 안전한 범위로 정규화한다")
	void normalizePagination() throws Exception {
		when(admissionCertificateService.getAdmissionCertificateList(
			eq(AdmissionCertificateStatus.PENDING),
			any(Pageable.class)
		)).thenReturn(new AdmissionCertificateListResponse(
			List.of(),
			new PageInfo(0, PageRequests.MAX_SIZE, 0, 0, true, true)
		));

		mockMvc.perform(get("/api/admin/certificates/admission")
				.queryParam("status", "PENDING")
				.queryParam("page", "-1")
				.queryParam("size", "1000000"))
			.andExpect(status().isOk());

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(admissionCertificateService).getAdmissionCertificateList(
			eq(AdmissionCertificateStatus.PENDING),
			pageableCaptor.capture()
		);
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize())
			.isEqualTo(PageRequests.MAX_SIZE);
	}

	@Test
	@DisplayName("승인한 관리자 ID를 서비스에 전달한다")
	void approvePassesAdminId() {
		AdminOidcUser principal = org.mockito.Mockito.mock(AdminOidcUser.class);
		when(principal.getAdminId()).thenReturn(7L);

		controller.approveAdmissionCertificateStatus(principal, 3001L);

		verify(admissionCertificateService).approveAdmissionCertificate(3001L, 7L);
	}

	@Test
	@DisplayName("반려한 관리자 ID와 사유를 서비스에 전달한다")
	void rejectPassesAdminIdAndReason() {
		AdminOidcUser principal = org.mockito.Mockito.mock(AdminOidcUser.class);
		when(principal.getAdminId()).thenReturn(7L);
		AdmissionCertificateRejectRequest request =
			new AdmissionCertificateRejectRequest("식별 정보가 선명하지 않습니다.");

		controller.rejectAdmissionCertificateStatus(principal, 3001L, request);

		verify(admissionCertificateService).rejectAdmissionCertificate(
			3001L,
			7L,
			"식별 정보가 선명하지 않습니다."
		);
	}
}
