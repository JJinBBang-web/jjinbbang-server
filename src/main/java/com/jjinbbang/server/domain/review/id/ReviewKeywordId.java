package com.jjinbbang.server.domain.review.id;

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

	@Column(name = "review_id")
	private Long reviewId;

	@Column(name = "keyword_id")
	private Long keywordId;
}
