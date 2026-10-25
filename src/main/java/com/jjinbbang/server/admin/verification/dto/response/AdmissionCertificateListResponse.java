package com.jjinbbang.server.admin.verification.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.jjinbbang.server.admin.verification.entity.AdmissionCertificate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.data.domain.Page;

public record AdmissionCertificateListResponse(
	List<CertificateSummary> certificateList,
	PageInfo pageInfo
) {
	/** 엔티티 페이지와 재업로드 판정 결과를 API 응답 형식으로 변환한다. */
	public static AdmissionCertificateListResponse from(
		Page<AdmissionCertificate> certificates,
		Set<Long> reuploadedCertificateIds
	) {
		List<CertificateSummary> certificateList = certificates.getContent().stream()
			.map(certificate -> CertificateSummary.from(
				certificate,
				reuploadedCertificateIds.contains(certificate.getId())
			))
			.toList();

		return new AdmissionCertificateListResponse(
			certificateList,
			PageInfo.from(certificates)
		);
	}

	public record CertificateSummary(
		Long certificateId,
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime createdAt,
		Long userId,
		@JsonProperty("isReuploaded") boolean isReuploaded,
		String schoolName
	) {
		private static CertificateSummary from(
			AdmissionCertificate certificate,
			boolean isReuploaded
		) {
			return new CertificateSummary(
				certificate.getId(),
				certificate.getCreatedAt(),
				certificate.getUser().getId(),
				isReuploaded,
				certificate.getUser().getUniversity().getName()
			);
		}
	}


	public record PageInfo(
		int currentPage,
		int pageSize,
		long totalElements,
		int totalPages,
		@JsonProperty("isFirst") boolean isFirst,
		@JsonProperty("isLast") boolean isLast
	) {
		/** Spring Data의 페이지 메타데이터를 클라이언트 응답 규격으로 옮긴다. */
		private static PageInfo from(Page<?> page) {
			return new PageInfo(
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast()
			);
		}
	}
}
