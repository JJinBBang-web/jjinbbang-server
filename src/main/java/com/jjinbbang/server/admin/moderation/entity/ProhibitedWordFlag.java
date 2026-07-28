package com.jjinbbang.server.admin.moderation.entity;

import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.global.persistence.CreatedAtEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "prohibited_word_flags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProhibitedWordFlag extends CreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "prohibited_words_id", nullable = false)
	private ProhibitedWord prohibitedWord;
}
