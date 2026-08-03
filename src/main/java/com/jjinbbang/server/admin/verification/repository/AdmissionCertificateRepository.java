package com.jjinbbang.server.admin.verification.repository;

import java.util.Collection;
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

	//현재 증명서보다 ID가 작은 동일 사용자의 증명서가 있으면 재업로드로 판단한다.
	@Query("""
		SELECT certificate.id
		FROM AdmissionCertificate certificate
		WHERE certificate.id IN :certificateIds
		  AND EXISTS (
		      SELECT previous.id
		      FROM AdmissionCertificate previous
		      WHERE previous.user = certificate.user
		        AND previous.id < certificate.id
		  )
		""")
	Set<Long> findReuploadedCertificateIds(@Param("certificateIds") Collection<Long> certificateIds);
}
