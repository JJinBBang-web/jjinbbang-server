package com.jjinbbang.server.admin.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.jjinbbang.server.admin.verification.dto.response.AdmissionCertificateListResponse;
import com.jjinbbang.server.admin.verification.entity.AdmissionCertificate;
import com.jjinbbang.server.admin.verification.repository.AdmissionCertificateRepository;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;
import com.jjinbbang.server.domain.common.entity.University;
import com.jjinbbang.server.domain.user.entity.User;

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
