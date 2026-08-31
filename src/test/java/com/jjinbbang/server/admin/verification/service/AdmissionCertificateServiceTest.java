package com.jjinbbang.server.admin.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.jjinbbang.server.admin.administrator.entity.Admin;
import com.jjinbbang.server.admin.administrator.repository.AdminRepository;
import com.jjinbbang.server.admin.administrator.type.AdminStatus;
import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse;
import com.jjinbbang.server.admin.verification.entity.AdmissionCertificate;
import com.jjinbbang.server.admin.verification.exception.AdmissionCertificateErrorCode;
import com.jjinbbang.server.admin.verification.repository.AdmissionCertificateRepository;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;
import com.jjinbbang.server.domain.common.entity.University;
import com.jjinbbang.server.domain.user.entity.User;
import com.jjinbbang.server.domain.user.repository.UserRepository;
import com.jjinbbang.server.global.error.BusinessException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@DisplayName("합격증명서 목록 조회 서비스")
class AdmissionCertificateServiceTest {

	@Mock
	AdmissionCertificateRepository admissionCertificateRepository;

	@Mock
	UserRepository userRepository;

	@Mock
	AdminRepository adminRepository;

	@InjectMocks
	AdmissionCertificateService admissionCertificateService;

	@Test
	@DisplayName("상태별 목록을 최신순으로 조회하고 재업로드 여부와 페이지 정보를 반환한다")
	void getAdmissionCertificateList() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 6, 5, 5, 10);
		AdmissionCertificate certificate = certificate(3001L, createdAt, 10021L, "전남대");
		Page<AdmissionCertificate> page = new PageImpl<>(
			List.of(certificate),
			PageRequest.of(0, 10),
			1
		);

		when(admissionCertificateRepository.findAllByStatus(
			any(AdmissionCertificateStatus.class),
			any(Pageable.class)
		)).thenReturn(page);
		when(admissionCertificateRepository.findReuploadedCertificateIds(List.of(3001L)))
			.thenReturn(Set.of(3001L));

		AdmissionCertificateListResponse response = admissionCertificateService
			.getAdmissionCertificateList(AdmissionCertificateStatus.PENDING, 0, 10);

		assertThat(response.certificateList()).singleElement().satisfies(summary -> {
			assertThat(summary.certificateId()).isEqualTo(3001L);
			assertThat(summary.createdAt()).isEqualTo(createdAt);
			assertThat(summary.userId()).isEqualTo(10021L);
			assertThat(summary.isReuploaded()).isTrue();
			assertThat(summary.schoolName()).isEqualTo("전남대");
		});
		assertThat(response.pageInfo().currentPage()).isZero();
		assertThat(response.pageInfo().pageSize()).isEqualTo(10);
		assertThat(response.pageInfo().totalElements()).isEqualTo(1);
		assertThat(response.pageInfo().totalPages()).isEqualTo(1);
		assertThat(response.pageInfo().isFirst()).isTrue();
		assertThat(response.pageInfo().isLast()).isTrue();

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(admissionCertificateRepository).findAllByStatus(
			org.mockito.ArgumentMatchers.eq(AdmissionCertificateStatus.PENDING),
			pageableCaptor.capture()
		);
		Sort sort = pageableCaptor.getValue().getSort();
		assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	@DisplayName("빈 페이지에서는 재업로드 여부 조회를 생략한다")
	void emptyPageDoesNotQueryReuploadedCertificates() {
		Page<AdmissionCertificate> emptyPage = Page.empty(PageRequest.of(2, 10));
		when(admissionCertificateRepository.findAllByStatus(
			any(AdmissionCertificateStatus.class),
			any(Pageable.class)
		)).thenReturn(emptyPage);

		AdmissionCertificateListResponse response = admissionCertificateService
			.getAdmissionCertificateList(AdmissionCertificateStatus.REJECT, 2, 10);

		assertThat(response.certificateList()).isEmpty();
		assertThat(response.pageInfo().currentPage()).isEqualTo(2);
		verify(admissionCertificateRepository, never()).findReuploadedCertificateIds(any());
	}

	@Test
	@DisplayName("최신 증명서를 처리한 관리자와 사용자 인증 정보를 함께 기록한다")
	void approveLatestCertificateWithAdmin() {
		Long certificateId = 3001L;
		Long userId = 10021L;
		Long adminId = 7L;
		LocalDateTime createdAt = LocalDateTime.of(2026, 6, 5, 5, 10);
		AdmissionCertificate certificate = mock(AdmissionCertificate.class);
		User user = mock(User.class);
		Admin admin = mock(Admin.class);

		when(admissionCertificateRepository.findUserIdByCertificateId(certificateId))
			.thenReturn(java.util.Optional.of(userId));
		when(userRepository.findByIdForUpdate(userId)).thenReturn(java.util.Optional.of(user));
		when(admissionCertificateRepository.findByIdForUpdate(certificateId))
			.thenReturn(java.util.Optional.of(certificate));
		when(certificate.getId()).thenReturn(certificateId);
		when(certificate.getCreatedAt()).thenReturn(createdAt);
		when(certificate.getUrl()).thenReturn("https://example.com/certificate.png");
		when(admissionCertificateRepository.existsNewerSubmission(userId, createdAt, certificateId))
			.thenReturn(false);
		when(adminRepository.findByIdAndStatus(adminId, AdminStatus.ACTIVE))
			.thenReturn(java.util.Optional.of(admin));

		admissionCertificateService.approveAdmissionCertificate(certificateId, adminId);

		verify(certificate).approve(admin);
		verify(user).approveAdmissionCertificate("https://example.com/certificate.png", createdAt);
	}

	@Test
	@DisplayName("더 최신 제출이 있으면 오래된 증명서를 처리하지 않는다")
	void staleCertificateCannotBeProcessed() {
		Long certificateId = 3001L;
		Long userId = 10021L;
		LocalDateTime createdAt = LocalDateTime.of(2026, 6, 5, 5, 10);
		AdmissionCertificate certificate = mock(AdmissionCertificate.class);
		User user = mock(User.class);

		when(admissionCertificateRepository.findUserIdByCertificateId(certificateId))
			.thenReturn(java.util.Optional.of(userId));
		when(userRepository.findByIdForUpdate(userId)).thenReturn(java.util.Optional.of(user));
		when(admissionCertificateRepository.findByIdForUpdate(certificateId))
			.thenReturn(java.util.Optional.of(certificate));
		when(certificate.getId()).thenReturn(certificateId);
		when(certificate.getCreatedAt()).thenReturn(createdAt);
		when(admissionCertificateRepository.existsNewerSubmission(userId, createdAt, certificateId))
			.thenReturn(true);

		assertThatThrownBy(() ->
			admissionCertificateService.rejectAdmissionCertificate(certificateId, 7L, "식별 불가")
		)
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode())
					.isEqualTo(AdmissionCertificateErrorCode.ADMISSION_CERTIFICATE_SUPERSEDED)
			);

		verify(certificate, never()).reject(any(), any());
		verifyNoInteractions(adminRepository);
		verify(user, never()).rejectAdmissionCertificate();
	}

	private AdmissionCertificate certificate(
		Long certificateId,
		LocalDateTime createdAt,
		Long userId,
		String schoolName
	) {
		AdmissionCertificate certificate = mock(AdmissionCertificate.class);
		User user = mock(User.class);
		University university = mock(University.class);

		when(certificate.getId()).thenReturn(certificateId);
		when(certificate.getCreatedAt()).thenReturn(createdAt);
		when(certificate.getUser()).thenReturn(user);
		when(user.getId()).thenReturn(userId);
		when(user.getUniversity()).thenReturn(university);
		when(university.getName()).thenReturn(schoolName);

		return certificate;
	}
}
