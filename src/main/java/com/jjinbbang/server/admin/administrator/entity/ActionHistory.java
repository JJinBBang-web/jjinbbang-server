package com.jjinbbang.server.admin.administrator.entity;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.global.persistence.CreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "action_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionHistory extends CreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id")
	private Review review;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_id")
	private Report report;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "admin_id", nullable = false)
	private Admin admin;

	@Column(nullable = false, length = 255)
	private String reason;

	@Lob
	@Column(name = "detail_reason", columnDefinition = "TEXT")
	private String detailReason;

	/** 리뷰에 대한 조치(마스킹 등)를 기록한다. {@code report}는 이 조치와 무관하므로 비워 둔다. */
	public static ActionHistory forReview(Review review, Admin admin, String reason, String detailReason) {
		ActionHistory actionHistory = new ActionHistory();
		actionHistory.review = review;
		actionHistory.admin = admin;
		actionHistory.reason = reason;
		actionHistory.detailReason = detailReason;
		return actionHistory;
	}
}
