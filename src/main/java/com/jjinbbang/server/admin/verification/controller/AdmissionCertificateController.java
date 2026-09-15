package com.jjinbbang.server.admin.verification.controller;

import com.jjinbbang.server.admin.administrator.security.AdminOidcUser;
import com.jjinbbang.server.admin.verification.dto.request.AdmissionCertificateRejectRequest;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateResponse;
import com.jjinbbang.server.admin.verification.service.AdmissionCertificateService;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;
import com.jjinbbang.server.global.paging.PageRequests;
import com.jjinbbang.server.global.template.ResTemplate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/certificates/admission")
public class AdmissionCertificateController {

	private static final Sort LATEST_FIRST = Sort.by(
		Sort.Order.desc("createdAt"),
		Sort.Order.desc("id")
	);

	private final AdmissionCertificateService admissionCertificateService;

	/**
	 * 합격증명서 목록을 상태별로 조회한다.
	 */
	@GetMapping
	public ResTemplate<AdmissionCertificateListResponse> getAdmissionCertificateList(
		@RequestParam(name = "status") AdmissionCertificateStatus status,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = PageRequests.DEFAULT_SIZE_PARAM) int size
	) {
		AdmissionCertificateListResponse response = admissionCertificateService
			.getAdmissionCertificateList(status, PageRequests.of(page, size, LATEST_FIRST));

		return new ResTemplate<>(HttpStatus.OK, "증명서 목록 조회 성공", response);
	}

	/**
	 * 합격증명서 ID로 상세 정보를 조회한다.
	 */
	@GetMapping("/{certificateId}")
	public ResTemplate<AdmissionCertificateResponse> getAdmissionCertificate(
		@PathVariable("certificateId") @Positive Long certificateId
	) {
		AdmissionCertificateResponse response = admissionCertificateService
			.getAdmissionCertificate(certificateId);

		return new ResTemplate<>(HttpStatus.OK, "증명서 상세 조회 성공", response);
	}

	/**
	 * 합격증명서를 승인하고 사용자를 신입생 인증 상태로 변경한다.
	 * 사용자의 합격증명서 URL과 업로드 일자, 인증 상태를 반영한다.
	 */
	@PatchMapping("/{certificateId}/approve")
	public ResTemplate<Void> approveAdmissionCertificateStatus(
		@AuthenticationPrincipal AdminOidcUser principal,
		@PathVariable("certificateId") @Positive Long certificateId
	) {
		admissionCertificateService.approveAdmissionCertificate(certificateId, principal.getAdminId());

		return new ResTemplate<>(HttpStatus.OK, "증명서 승인 성공");
	}

	/**
	 * 합격증명서를 반려하고 사용자를 미인증 상태로 변경한다.
	 * 합격증명서 상태는 REJECT, 사용자 인증 상태는 UNVERIFIED로 유지
	 */
	@PatchMapping("/{certificateId}/reject")
	public ResTemplate<Void> rejectAdmissionCertificateStatus(
		@AuthenticationPrincipal AdminOidcUser principal,
		@PathVariable("certificateId") @Positive Long certificateId,
		@Valid @RequestBody AdmissionCertificateRejectRequest request
	) {
		admissionCertificateService.rejectAdmissionCertificate(
			certificateId,
			principal.getAdminId(),
			request.rejectReason()
		);

		return new ResTemplate<>(HttpStatus.OK, "증명서 반려 성공");
	}
}
