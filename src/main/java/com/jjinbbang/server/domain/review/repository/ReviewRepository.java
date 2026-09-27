package com.jjinbbang.server.domain.review.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jjinbbang.server.domain.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

	/** 상세 조회는 작성자 학교·닉네임을 함께 내려줘야 해서 user·university를 fetch join한다. */
	@Query("""
		SELECT r FROM Review r
		JOIN FETCH r.user u
		JOIN FETCH u.university
		WHERE r.id = :reviewId
		""")
	Optional<Review> findDetailById(@Param("reviewId") Long reviewId);
}
