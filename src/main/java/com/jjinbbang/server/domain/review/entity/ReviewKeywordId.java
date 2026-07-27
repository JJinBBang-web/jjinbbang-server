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
public class ReviewKeywordId implements Serializable {

	@Column(name = "id")
	private Long reviewId;

	@Column(name = "id2")
	private Long keywordId;
}
