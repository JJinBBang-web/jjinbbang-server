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

	/** 후기 원문. 마스킹해도 지우지 않는다 — 마스킹본은 {@link #maskedContent}에 따로 둔다. */
	@Lob
	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String originalContent;

	/** 마스킹본. {@code null}이면 마스킹되지 않은 상태다. */
	@Lob
	@Column(name = "masked_content", columnDefinition = "TEXT")
	private String maskedContent;

	@Column(name = "like_count", nullable = false)
	private Integer likeCount;

	@Column(name = "prohibited_word_flag", nullable = false)
	private boolean prohibitedWordFlag;

	public void changeStatus(ReviewStatus status) {
		this.status = status;
	}

	/**
	 * 사용자와 관리자 화면에 보여줄 본문. 마스킹본이 있으면 마스킹본, 없으면 원문이다.
	 *
	 * <p>원문이 필요한 곳(마스킹 계산, 키워드 검색)만 {@link #getOriginalContent()}를 명시적으로 부른다.
	 * 기본 getter가 마스킹본을 먼저 돌려주므로 원문이 실수로 응답에 나가는 일을 줄인다.
	 */
	public String getContent() {
		return maskedContent != null ? maskedContent : originalContent;
	}

	public boolean isMasked() {
		return maskedContent != null;
	}

	/** 마스킹을 확정한다. 원문은 그대로 두고 마스킹본을 저장하며 금칙어 플래그를 세운다. */
	public void mask(String maskedContent) {
		this.maskedContent = maskedContent;
		this.prohibitedWordFlag = true;
	}

	/** 마스킹을 되돌린다. 마스킹본을 비워 원문이 다시 보이게 하고 금칙어 플래그를 내린다. */
	public void unmask() {
		this.maskedContent = null;
		this.prohibitedWordFlag = false;
	}
}
