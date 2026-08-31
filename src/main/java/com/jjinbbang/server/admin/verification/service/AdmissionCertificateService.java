package com.jjinbbang.server.admin.verification.service;

import java.util.List;
import java.util.Set;

import com.jjinbbang.server.admin.administrator.entity.Admin;
import com.jjinbbang.server.admin.administrator.exception.AdminAuthenticationErrorCode;
import com.jjinbbang.server.admin.administrator.repository.AdminRepository;
import com.jjinbbang.server.admin.administrator.type.AdminStatus;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateResponse;
import com.jjinbbang.server.admin.verification.entity.AdmissionCertificate;
import com.jjinbbang.server.admin.verification.exception.AdmissionCertificateErrorCode;
import com.jjinbbang.server.admin.verification.repository.AdmissionCertificateRepository;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;
import com.jjinbbang.server.domain.user.entity.User;
import com.jjinbbang.server.domain.user.repository.UserRepository;

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
	private final UserRepository userRepository;
	private final AdminRepository adminRepository;

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
	public void approveAdmissionCertificate(Long certificateId, Long adminId) {
		ProcessingContext context = prepareProcessing(certificateId, adminId);
		AdmissionCertificate certificate = context.certificate();

		certificate.approve(context.admin());
		context.user().approveAdmissionCertificate(
			certificate.getUrl(),
			certificate.getCreatedAt()
		);
	}

	@Transactional
	public void rejectAdmissionCertificate(Long certificateId, Long adminId, String rejectReason) {
		ProcessingContext context = prepareProcessing(certificateId, adminId);

		context.certificate().reject(context.admin(), rejectReason.strip());
		context.user().rejectAdmissionCertificate();
	}

	/** 사용자 단위로 처리 순서를 직렬화하고 최신 제출인지 확인한다. */
	private ProcessingContext prepareProcessing(Long certificateId, Long adminId) {
		Long userId = admissionCertificateRepository.findUserIdByCertificateId(certificateId)
			.orElseThrow(AdmissionCertificateErrorCode.ADMISSION_CERTIFICATE_NOT_FOUND::exception);
		User user = userRepository.findByIdForUpdate(userId)
			.orElseThrow(AdmissionCertificateErrorCode.ADMISSION_CERTIFICATE_NOT_FOUND::exception);
		AdmissionCertificate certificate = admissionCertificateRepository.findByIdForUpdate(certificateId)
			.orElseThrow(AdmissionCertificateErrorCode.ADMISSION_CERTIFICATE_NOT_FOUND::exception);

		if (admissionCertificateRepository.existsNewerSubmission(
			userId,
			certificate.getCreatedAt(),
			certificate.getId()
		)) {
			throw AdmissionCertificateErrorCode.ADMISSION_CERTIFICATE_SUPERSEDED.exception();
		}

		Admin admin = adminRepository.findByIdAndStatus(adminId, AdminStatus.ACTIVE)
			.orElseThrow(AdminAuthenticationErrorCode.AUTHENTICATION_REQUIRED::exception);
		return new ProcessingContext(certificate, user, admin);
	}

	private record ProcessingContext(
		AdmissionCertificate certificate,
		User user,
		Admin admin
	) {
	}
}
