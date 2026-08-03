package com.jjinbbang.server.admin.verification.controller;

import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateResponse;
import com.jjinbbang.server.admin.verification.service.AdmissionCertificateService;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;
import com.jjinbbang.server.global.template.ResTemplate;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/certificates/admission")
public class AdmissionCertificateController {

	private final AdmissionCertificateService admissionCertificateService;

	/**
	 * 합격증명서 목록을 상태별로 조회한다.
	 * TODO: 관리자 인증이 추가되면 권한 확인
	 */
	@GetMapping
	public ResTemplate<AdmissionCertificateListResponse> getAdmissionCertificateList(
		@RequestParam(name = "status") AdmissionCertificateStatus status,
		@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
		@RequestParam(name = "size", defaultValue = "10") @Min(1) int size
	) {
		AdmissionCertificateListResponse response = admissionCertificateService
			.getAdmissionCertificateList(status, page, size);

		return new ResTemplate<>(HttpStatus.OK, "증명서 목록 조회 성공", response);
	}

	/**
	 * 합격증명서 ID로 상세 정보를 조회한다.
	 * TODO: 관리자 인증이 추가되면 권한 확인
	 */
	@GetMapping("/{certificateId}")
	public ResTemplate<AdmissionCertificateResponse> getAdmissionCertificate(
		@PathVariable("certificateId") @Positive Long certificateId
	) {
		AdmissionCertificateResponse response = admissionCertificateService
			.getAdmissionCertificate(certificateId);

		return new ResTemplate<>(HttpStatus.OK, "증명서 상세 조회 성공", response);
	}
}
