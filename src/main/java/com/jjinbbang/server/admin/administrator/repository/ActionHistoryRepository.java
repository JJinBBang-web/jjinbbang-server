package com.jjinbbang.server.admin.administrator.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jjinbbang.server.admin.administrator.entity.ActionHistory;

/**
 * {@code ActionHistory}는 리뷰를 직접 참조하지 않고 {@code prohibitedWordFlag} 또는
 * {@code report} 중 하나를 거쳐서만 리뷰와 연결된다. 그래서 두 연관관계를 각각 LEFT JOIN 걸고
 * {@code OR}로 묶는다 — 한 행이 둘 중 어느 쪽으로 리뷰에 닿아도 걸리게 하려는 것.
 */
public interface ActionHistoryRepository extends JpaRepository<ActionHistory, Long> {

	@Query("""
		SELECT ah FROM ActionHistory ah
		JOIN FETCH ah.admin
		LEFT JOIN ah.prohibitedWordFlag pwf
		LEFT JOIN ah.report r
		WHERE pwf.review.id = :reviewId OR r.review.id = :reviewId
		ORDER BY ah.createdAt ASC
		""")
	List<ActionHistory> findByReviewIdOrderByCreatedAtAsc(@Param("reviewId") Long reviewId);
}
