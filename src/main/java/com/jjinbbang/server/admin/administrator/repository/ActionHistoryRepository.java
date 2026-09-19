package com.jjinbbang.server.admin.administrator.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jjinbbang.server.admin.administrator.entity.ActionHistory;

/**
 * {@code ActionHistory}는 리뷰에 대한 조치면 {@code review}를 직접 참조하고, 신고 처리에 대한
 * 조치면 {@code report}를 거쳐 리뷰와 연결된다. 그래서 두 경로를 {@code OR}로 묶어 조회한다 —
 * 한 행이 둘 중 어느 쪽으로 리뷰에 닿아도 걸리게 하려는 것.
 */
public interface ActionHistoryRepository extends JpaRepository<ActionHistory, Long> {

	@Query("""
		SELECT ah FROM ActionHistory ah
		JOIN FETCH ah.admin
		LEFT JOIN ah.report r
		WHERE ah.review.id = :reviewId OR r.review.id = :reviewId
		ORDER BY ah.createdAt ASC
		""")
	List<ActionHistory> findByReviewIdOrderByCreatedAtAsc(@Param("reviewId") Long reviewId);
}
