package com.jjinbbang.server.admin.moderation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;

/**
 * 신고 목록은 한 행마다 리뷰 본문과 신고자 소속을 함께 보여준다.
 * 연관관계가 전부 LAZY라서 fetch join 없이 조회하면 행 수만큼 추가 쿼리가 나간다.
 *
 * <p>상태 필터가 있고 없고를 {@code :status IS NULL}로 한 메서드에 몰지 않고 둘로 나눴다 —
 * 널 파라미터의 타입 추론에 기대지 않는 편이 안전하다.
 *
 * <p>{@code r.user}는 nullable(탈퇴 회원)이라 {@code LEFT JOIN}이다.
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

	@Query(
		value = """
			SELECT r FROM Report r
			JOIN FETCH r.review
			LEFT JOIN FETCH r.user u
			LEFT JOIN FETCH u.university
			""",
		countQuery = "SELECT COUNT(r) FROM Report r"
	)
	Page<Report> findPageWithDetails(Pageable pageable);

	@Query(
		value = """
			SELECT r FROM Report r
			JOIN FETCH r.review
			LEFT JOIN FETCH r.user u
			LEFT JOIN FETCH u.university
			WHERE r.status = :status
			""",
		countQuery = "SELECT COUNT(r) FROM Report r WHERE r.status = :status"
	)
	Page<Report> findPageByStatusWithDetails(@Param("status") ReportStatus status, Pageable pageable);
}
