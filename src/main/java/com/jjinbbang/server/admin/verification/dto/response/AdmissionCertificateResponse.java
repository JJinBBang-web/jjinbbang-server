package com.jjinbbang.server.admin.verification.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.jjinbbang.server.admin.verification.entity.AdmissionCertificate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record AdmissionCertificateResponse(
	Long certificateId,
	Long userId,
	String imageUrl,
	String schoolName,
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime createdAt,
	@JsonFormat(pattern = "yyyy-MM-dd") LocalDate userCreatedAt,
	String email,
	Long reuploadedId
) {
	public static AdmissionCertificateResponse from(
		AdmissionCertificate certificate,
		Long reuploadedId
	) {
		return new AdmissionCertificateResponse(
			certificate.getId(),
			certificate.getUser().getId(),
			certificate.getUrl(),
			certificate.getUser().getUniversity().getName(),
			certificate.getCreatedAt(),
			certificate.getUser().getCreatedAt().toLocalDate(),
			maskEmail(certificate.getUser().getUniversityEmail()),
			reuploadedId
		);
	}

	/** 이메일 아이디의 앞 네 글자만 남기고 나머지는 마스킹한다. */
	private static String maskEmail(String email) {
		if (email == null || email.isBlank()) {
			return null;
		}

		int atIndex = email.indexOf('@');
		if (atIndex <= 0 || atIndex == email.length() - 1) {
			return "***";
		}

		String localPart = email.substring(0, atIndex);
		int visibleLength = Math.min(4, localPart.length());

		return localPart.substring(0, visibleLength) + "***" + email.substring(atIndex);
	}
}
