package com.jjinbbang.server.domain.review.entity;

import com.jjinbbang.server.domain.common.entity.Keyword;
import com.jjinbbang.server.domain.review.id.ReviewKeywordId;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "review_keywords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewKeyword {

	@EmbeddedId
	private ReviewKeywordId id;

	@MapsId("reviewId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@MapsId("keywordId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "keyword_id", nullable = false)
	private Keyword keyword;
}
