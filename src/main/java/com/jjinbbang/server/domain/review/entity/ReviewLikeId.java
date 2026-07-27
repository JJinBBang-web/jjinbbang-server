package com.jjinbbang.server.domain.review.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewLikeId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "review_id")
	private Long reviewId;
}
