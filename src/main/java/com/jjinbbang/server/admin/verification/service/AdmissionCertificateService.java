package com.jjinbbang.server.admin.verification.service;

import java.util.List;
import java.util.Set;

import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateResponse;
import com.jjinbbang.server.admin.verification.entity.AdmissionCertificate;
import com.jjinbbang.server.admin.verification.exception.AdmissionCertificateErrorCode;
import com.jjinbbang.server.admin.verification.repository.AdmissionCertificateRepository;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionCertificateService {

	private final AdmissionCertificateRepository admissionCertificateRepository;

	public AdmissionCertificateListResponse getAdmissionCertificateList(
		AdmissionCertificateStatus status,
		int page,
		int size
	) {
		// 정렬 기준: 신청 시각, ID
		PageRequest pageRequest = PageRequest.of(
			page,
			size,
			Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
		);
		Page<AdmissionCertificate> certificates = admissionCertificateRepository
			.findAllByStatus(status, pageRequest);

		// 현재 페이지의 증명서만 한 번에 확인해 항목별 추가 쿼리(N+1)를 피한다.
		List<Long> certificateIds = certificates.getContent().stream()
			.map(AdmissionCertificate::getId)
			.toList();
		// 빈 IN 절이 만들어지지 않도록 조회 결과가 없으면 재업로드 쿼리를 실행하지 않는다.
		Set<Long> reuploadedCertificateIds = certificateIds.isEmpty()
			? Set.of()
			: admissionCertificateRepository.findReuploadedCertificateIds(certificateIds);

		return AdmissionCertificateListResponse.from(certificates, reuploadedCertificateIds);
	}

	public AdmissionCertificateResponse getAdmissionCertificate(Long certificateId) {
		AdmissionCertificate certificate = admissionCertificateRepository.findDetailById(certificateId)
			.orElseThrow(AdmissionCertificateErrorCode.ADMISSION_CERTIFICATE_NOT_FOUND::exception);

		// 신청 시각이 가장 가까운 이전 제출을 재업로드 원본으로 사용한다.
		Long reuploadedId = admissionCertificateRepository.findPreviousCertificateIds(
			certificate.getUser().getId(),
			certificate.getCreatedAt(),
			certificate.getId(),
			PageRequest.of(0, 1)
		).stream().findFirst().orElse(null);

		return AdmissionCertificateResponse.from(certificate, reuploadedId);
	}

	@Transactional
	public void approveAdmissionCertificate(Long certificateId) {
		// 동시에 같은 증명서를 처리하지 못하도록 쓰기 잠금과 함께 조회한다.
		AdmissionCertificate certificate = admissionCertificateRepository.findByIdForUpdate(certificateId)
			.orElseThrow(AdmissionCertificateErrorCode.ADMISSION_CERTIFICATE_NOT_FOUND::exception);

		certificate.approve();
		certificate.getUser().approveAdmissionCertificate(
			certificate.getUrl(),
			certificate.getCreatedAt()
		);
	}

	@Transactional
	public void rejectAdmissionCertificate(Long certificateId) {
		// 승인과 마찬가지로 동일 증명서의 중복 처리를 쓰기 잠금으로 막는다.
		AdmissionCertificate certificate = admissionCertificateRepository.findByIdForUpdate(certificateId)
			.orElseThrow(AdmissionCertificateErrorCode.ADMISSION_CERTIFICATE_NOT_FOUND::exception);

		certificate.reject();
		certificate.getUser().rejectAdmissionCertificate();
	}
}
