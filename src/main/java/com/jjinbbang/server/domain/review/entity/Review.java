package com.jjinbbang.server.domain.review.entity;

import com.jjinbbang.server.domain.agency.entity.Agency;
import com.jjinbbang.server.domain.building.entity.Building;
import com.jjinbbang.server.domain.review.type.ReviewStatus;
import com.jjinbbang.server.domain.review.type.ReviewType;
import com.jjinbbang.server.domain.user.entity.User;
import com.jjinbbang.server.global.persistence.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends SoftDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "building_id")
	private Building building;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "agency_id")
	private Agency agency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReviewStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ReviewType type;

	@Column(nullable = false)
	private Integer rating;

	@Column(name = "thumbnail_image", length = 2048)
	private String thumbnailImage;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "like_count", nullable = false)
	private Integer likeCount;

	@Column(name = "prohibited_word_flag", nullable = false)
	private boolean prohibitedWordFlag;

	public void changeStatus(ReviewStatus status) {
		this.status = status;
	}

	/** 마스킹을 확정하면서 본문을 치환하고 금칙어 플래그를 세운다. */
	public void mask(String maskedContent) {
		this.content = maskedContent;
		this.prohibitedWordFlag = true;
	}
}
