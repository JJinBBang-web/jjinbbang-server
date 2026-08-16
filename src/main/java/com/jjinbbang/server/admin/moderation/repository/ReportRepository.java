package com.jjinbbang.server.admin.moderation.repository;

import java.util.List;

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
 * <p>{@code r.user}가 {@code LEFT JOIN}인 것은 {@code reports.user_id}가 nullable이기 때문이다.
 * <b>무엇이 그 null을 만드는지는 아직 확정되지 않았다</b> — 회원 탈퇴는 소프트 삭제라
 * ({@code users.deleted_at}) 행이 남으므로 그것만으로는 null이 되지 않고, V2에는 탈퇴·삭제 흐름 자체가 없다.
 * 이어지는 {@code u.university}도 {@code LEFT}여야 한다 — {@code users.university_id}는 {@code NOT NULL}이지만
 * 조인 대상이 {@code u}라서, {@code u}가 null인 행이 통째로 탈락한다.
 *
 * <p>소프트 삭제된 유저·리뷰는 <b>의도적으로 걸러내지 않는다.</b> 어드민이 "이 신고가 정당한가"를
 * 판단하려면 신고자와 대상 리뷰가 보여야 한다. 걸러내면 신고만 남고 무엇을 신고한 건지 알 수 없다.
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

	/** 리뷰 목록에서 행마다 신고 건수를 보여주려고 배치로 센다. {@code row[0]}은 {@code reviewId}, {@code row[1]}은 건수다. */
	@Query("SELECT r.review.id, COUNT(r) FROM Report r WHERE r.review.id IN :reviewIds GROUP BY r.review.id")
	List<Object[]> countByReviewIdIn(@Param("reviewIds") List<Long> reviewIds);

	/**
	 * 리뷰 상세 화면의 신고 목록. {@code r.user}가 {@code LEFT JOIN}인 이유는
	 * {@link #findPageWithDetails}와 같다 — {@code reports.user_id}가 nullable이다.
	 */
	@Query("""
		SELECT r FROM Report r
		LEFT JOIN FETCH r.user
		WHERE r.review.id = :reviewId
		ORDER BY r.createdAt ASC
		""")
	List<Report> findByReviewIdOrderByCreatedAtAsc(@Param("reviewId") Long reviewId);
}
