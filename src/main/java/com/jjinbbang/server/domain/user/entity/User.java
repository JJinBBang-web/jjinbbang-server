package com.jjinbbang.server.domain.user.entity;

import java.time.LocalDateTime;

import com.jjinbbang.server.domain.common.entity.University;
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

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "university_id", nullable = false)
	private University university;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Provider provider;

	@Column(name = "provider_id", nullable = false, length = 100)
	private String providerId;

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
}
