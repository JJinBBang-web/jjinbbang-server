package com.jjinbbang.server.admin.verification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse.CertificateSummary;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse.PageInfo;
import com.jjinbbang.server.admin.verification.service.AdmissionCertificateService;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;
import com.jjinbbang.server.global.error.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("합격증명서 목록 조회 API")
class AdmissionCertificateControllerTest {

	AdmissionCertificateService admissionCertificateService;
	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		admissionCertificateService = org.mockito.Mockito.mock(AdmissionCertificateService.class);
		AdmissionCertificateController controller = new AdmissionCertificateController(admissionCertificateService);
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
			AdmissionCertificateStatus.PENDING,
			0,
			10
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

		verify(admissionCertificateService).getAdmissionCertificateList(
			AdmissionCertificateStatus.PENDING,
			0,
			10
		);
	}

	@Test
	@DisplayName("page와 size를 생략하면 기본값을 사용한다")
	void defaultPagination() throws Exception {
		AdmissionCertificateListResponse response = new AdmissionCertificateListResponse(
			List.of(),
			new PageInfo(0, 10, 0, 0, true, true)
		);
		when(admissionCertificateService.getAdmissionCertificateList(
			AdmissionCertificateStatus.APPROVE,
			0,
			10
		)).thenReturn(response);

		mockMvc.perform(get("/api/admin/certificates/admission")
				.queryParam("status", "APPROVE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.certificateList").isEmpty());

		verify(admissionCertificateService).getAdmissionCertificateList(
			AdmissionCertificateStatus.APPROVE,
			0,
			10
		);
	}

	@Test
	@DisplayName("status가 없으면 400 INVALID_INPUT을 반환한다")
	void statusIsRequired() throws Exception {
		mockMvc.perform(get("/api/admin/certificates/admission"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
	}

	@Test
	@DisplayName("page가 음수이면 400 INVALID_INPUT을 반환한다")
	void pageMustNotBeNegative() throws Exception {
		mockMvc.perform(get("/api/admin/certificates/admission")
				.queryParam("status", "PENDING")
				.queryParam("page", "-1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
	}
}
