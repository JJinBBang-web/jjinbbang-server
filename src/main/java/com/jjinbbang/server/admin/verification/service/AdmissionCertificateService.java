package com.jjinbbang.server.admin.verification.service;

import java.util.List;
import java.util.Set;

import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse;
import com.jjinbbang.server.admin.verification.entity.AdmissionCertificate;
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
}
