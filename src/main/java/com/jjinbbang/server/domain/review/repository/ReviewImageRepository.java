package com.jjinbbang.server.domain.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jjinbbang.server.domain.review.entity.ReviewImage;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

	List<ReviewImage> findByReviewIdOrderBySortOrderAsc(Long reviewId);
}
