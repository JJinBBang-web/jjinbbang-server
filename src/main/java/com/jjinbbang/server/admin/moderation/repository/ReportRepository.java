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
 * 신고 목록은 <b>신고된 리뷰 단위</b>로 페이징하고, 각 리뷰 행 아래에 그 리뷰의 신고를 전부 붙인다.
 * 그래서 두 단계로 조회한다.
 * <ol>
 *   <li>{@code findReportedReviewIds*} — 리뷰 ID만 페이징한다. 정렬은 그 리뷰의 가장 최근 신고일 순이고,
 *       {@code Pageable}의 정렬은 쓰지 않는다 ({@code GROUP BY} 결과에 엔티티 속성 정렬을 붙일 수 없다).</li>
 *   <li>{@code findAllWithReviewByReviewIdIn*} — 그 페이지의 리뷰들에 달린 신고를 한 번에 모은다.
 *       리뷰마다 따로 조회하지 않으므로 N+1이 없다.</li>
 * </ol>
 *
 * <p>상태 필터는 두 단계 모두에 건다. 1단계에만 걸면 "접수" 탭에 이미 처리된 신고가 섞이고,
 * 2단계에만 걸면 처리된 신고만 남은 리뷰가 빈 행으로 페이지를 차지한다.
 *
 * <p>상태 필터가 있고 없고를 {@code :status IS NULL}로 한 메서드에 몰지 않고 둘로 나눴다 —
 * 널 파라미터의 타입 추론에 기대지 않는 편이 안전하다.
 *
 * <p>{@code r.user}가 {@code LEFT JOIN}인 것은 {@code reports.user_id}가 nullable이기 때문이다.
 * <b>무엇이 그 null을 만드는지는 아직 확정되지 않았다</b> — 회원 탈퇴는 소프트 삭제라
 * ({@code users.deleted_at}) 행이 남으므로 그것만으로는 null이 되지 않고, V2에는 탈퇴·삭제 흐름 자체가 없다.
 * 리뷰 작성자({@code rv.user})와 그 학교는 둘 다 {@code NOT NULL}이라 내부 조인이다.
 *
 * <p>소프트 삭제된 유저·리뷰는 <b>의도적으로 걸러내지 않는다.</b> 어드민이 "이 신고가 정당한가"를
 * 판단하려면 신고자와 대상 리뷰가 보여야 한다. 걸러내면 신고만 남고 무엇을 신고한 건지 알 수 없다.
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

	@Query(
		value = """
			SELECT r.review.id FROM Report r
			GROUP BY r.review.id
			ORDER BY MAX(r.createdAt) DESC, r.review.id DESC
			""",
		countQuery = "SELECT COUNT(DISTINCT r.review.id) FROM Report r"
	)
	Page<Long> findReportedReviewIds(Pageable pageable);

	@Query(
		value = """
			SELECT r.review.id FROM Report r
			WHERE r.status = :status
			GROUP BY r.review.id
			ORDER BY MAX(r.createdAt) DESC, r.review.id DESC
			""",
		countQuery = "SELECT COUNT(DISTINCT r.review.id) FROM Report r WHERE r.status = :status"
	)
	Page<Long> findReportedReviewIdsByStatus(@Param("status") ReportStatus status, Pageable pageable);

	@Query("""
		SELECT r FROM Report r
		JOIN FETCH r.review rv
		JOIN FETCH rv.user ru
		JOIN FETCH ru.university
		LEFT JOIN FETCH r.user
		WHERE rv.id IN :reviewIds
		ORDER BY r.createdAt ASC, r.id ASC
		""")
	List<Report> findAllWithReviewByReviewIdIn(@Param("reviewIds") List<Long> reviewIds);

	@Query("""
		SELECT r FROM Report r
		JOIN FETCH r.review rv
		JOIN FETCH rv.user ru
		JOIN FETCH ru.university
		LEFT JOIN FETCH r.user
		WHERE rv.id IN :reviewIds
		AND r.status = :status
		ORDER BY r.createdAt ASC, r.id ASC
		""")
	List<Report> findAllWithReviewByReviewIdInAndStatus(
		@Param("reviewIds") List<Long> reviewIds,
		@Param("status") ReportStatus status
	);

	/** 리뷰 목록에서 행마다 신고 건수를 보여주려고 배치로 센다. {@code row[0]}은 {@code reviewId}, {@code row[1]}은 건수다. */
	@Query("SELECT r.review.id, COUNT(r) FROM Report r WHERE r.review.id IN :reviewIds GROUP BY r.review.id")
	List<Object[]> countByReviewIdIn(@Param("reviewIds") List<Long> reviewIds);

	/**
	 * 리뷰 상세 화면의 신고 목록. {@code r.user}가 {@code LEFT JOIN}인 이유는
	 * 클래스 주석과 같다 — {@code reports.user_id}가 nullable이다.
	 */
	@Query("""
		SELECT r FROM Report r
		LEFT JOIN FETCH r.user
		WHERE r.review.id = :reviewId
		ORDER BY r.createdAt ASC
		""")
	List<Report> findByReviewIdOrderByCreatedAtAsc(@Param("reviewId") Long reviewId);
}
