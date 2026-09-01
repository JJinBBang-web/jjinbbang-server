package com.jjinbbang.server.admin.moderation.entity;

import com.jjinbbang.server.admin.moderation.type.ReportStatus;
import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.domain.user.entity.User;
import com.jjinbbang.server.global.persistence.CreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends CreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@Column(nullable = false, length = 50)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReportStatus status;

	/**
	 * 신고를 기각한다. 대상 리뷰는 그대로 두고 신고만 반려 상태로 바꾼다.
	 *
	 * <p>이미 처리된 신고인지는 {@link #isHandled()}로 서비스가 먼저 확인한다.
	 */
	public void dismiss() {
		this.status = ReportStatus.REJECT;
	}

	/** 관리자가 이미 승인 또는 기각한 신고인가. */
	public boolean isHandled() {
		return status != ReportStatus.PENDING;
	}
}
