package com.jjinbbang.server.admin.moderation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jjinbbang.server.admin.moderation.entity.ProhibitedWordFlag;

/**
 * 리뷰 목록 행마다 금칙어 배지를 표시하는 데 쓴다. "금칙어만 보기" 필터 자체는
 * {@code ReviewSpecifications}가 {@code EXISTS} 서브쿼리로 처리해서 여기 별도 메서드가 없다.
 *
 * <p>플래그 여부만 필요해서 {@code review.id}만 뽑는다 — {@code ProhibitedWordFlag} 자체를
 * fetch join으로 끌어올 이유가 없다.
 */
public interface ProhibitedWordFlagRepository extends JpaRepository<ProhibitedWordFlag, Long> {

	@Query("SELECT DISTINCT f.review.id FROM ProhibitedWordFlag f WHERE f.review.id IN :reviewIds")
	List<Long> findFlaggedReviewIds(@Param("reviewIds") List<Long> reviewIds);

	/** 리뷰 상세 조회는 리뷰 한 건만 확인하면 되므로 배치용 {@link #findFlaggedReviewIds}를 쓰지 않는다. */
	boolean existsByReviewId(Long reviewId);
}
