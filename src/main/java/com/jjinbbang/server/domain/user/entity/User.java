package com.jjinbbang.server.domain.user.entity;

import java.time.LocalDateTime;

import com.jjinbbang.server.domain.common.entity.University;
import com.jjinbbang.server.domain.user.type.Provider;
import com.jjinbbang.server.domain.user.type.VerificationStatus;
import com.jjinbbang.server.global.persistence.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "users",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_users_provider_identity",
		columnNames = {"provider", "provider_id"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeleteEntity {

	private static final String DEFAULT_NICKNAME = "익명의 찐빵이";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "university_id", nullable = false)
	private University university;

	@Column(length = 50)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Provider provider;

	@Column(name = "provider_id", nullable = false, length = 100)
	private String providerId;

	@Column(nullable = false, length = 20)
	private String nickname = DEFAULT_NICKNAME;

	@Column(name = "student_number", length = 50)
	private String studentNumber;

	@Column(name = "university_email")
	private String universityEmail;

	@Column(name = "admission_certificate", length = 2048)
	private String admissionCertificate;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_status", nullable = false, length = 20)
	private VerificationStatus verificationStatus;

	@Column(name = "certificate_upload_date")
	private LocalDateTime certificateUploadDate;

	/** 승인된 합격증명서 정보를 사용자 인증 상태에 반영한다. */
	public void approveAdmissionCertificate(String certificateUrl, LocalDateTime uploadedAt) {
		admissionCertificate = certificateUrl;
		verificationStatus = VerificationStatus.NEW_STUDENT_VERIFIED;
		certificateUploadDate = uploadedAt;
	}

	/** 반려된 증명서가 사용자 인증 정보에 남지 않도록 미인증 상태로 초기화한다. */
	public void rejectAdmissionCertificate() {
		admissionCertificate = null;
		verificationStatus = VerificationStatus.UNVERIFIED;
		certificateUploadDate = null;
	}
}
