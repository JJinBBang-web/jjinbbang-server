package com.jjinbbang.server.admin.verification.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.jjinbbang.server.admin.verification.entity.AdmissionCertificate;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdmissionCertificateRepository extends JpaRepository<AdmissionCertificate, Long> {

	// 합격증명서와 사용자, 학교를 한 쿼리에서 조회
	@EntityGraph(attributePaths = {"user", "user.university"})
	Page<AdmissionCertificate> findAllByStatus(AdmissionCertificateStatus status, Pageable pageable);

	// 동일 사용자의 더 이른 신청이 있으면 재업로드로 판단한다.(날짜 기준 + 동일한 경우 ID)
	@Query("""
		SELECT certificate.id
		FROM AdmissionCertificate certificate
		WHERE certificate.id IN :certificateIds
		  AND EXISTS (
		      SELECT previous.id
		      FROM AdmissionCertificate previous
		      WHERE previous.user = certificate.user
		        AND (
		            previous.createdAt < certificate.createdAt
		            OR (
		                previous.createdAt = certificate.createdAt
		                AND previous.id < certificate.id
		            )
		        )
		  )
		""")
	Set<Long> findReuploadedCertificateIds(@Param("certificateIds") Collection<Long> certificateIds);

	// 합격증명서 데이터에 연관된 사용자와 학교를 함께 조회한다.
	@EntityGraph(attributePaths = {"user", "user.university"})
	@Query("""
		SELECT certificate
		FROM AdmissionCertificate certificate
		WHERE certificate.id = :certificateId
		""")
	Optional<AdmissionCertificate> findDetailById(@Param("certificateId") Long certificateId);

	// 가장 최근 제출내역 조회(날짜 기준 + 동일한 경우 ID)
	@Query("""
		SELECT previous.id
		FROM AdmissionCertificate previous
		WHERE previous.user.id = :userId
		  AND (
		      previous.createdAt < :createdAt
		      OR (
		          previous.createdAt = :createdAt
		          AND previous.id < :certificateId
		      )
		  )
		ORDER BY previous.createdAt DESC, previous.id DESC
		""")
	List<Long> findPreviousCertificateIds(
		@Param("userId") Long userId,
		@Param("createdAt") LocalDateTime createdAt,
		@Param("certificateId") Long certificateId,
		Pageable pageable
	);
}
