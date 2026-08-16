package com.jjinbbang.server.admin.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.jjinbbang.server.admin.administrator.entity.ActionHistory;
import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.domain.review.type.ReviewStatus;
import com.jjinbbang.server.domain.user.entity.User;

/**
 * 리뷰 상세 한 건.
 *
 * <p>스키마 때문에 명세서와 달라진 곳이 하나 있다 — {@code reviews}에 제목 컬럼이 없어
 * {@link ReviewResponse}·{@code ReportResponse}와 같은 이유로 title은 내려주지 않는다.
 *
 * <p>{@code historyList}는 {@link ActionHistory}를 내려주는데, 이 엔티티엔 {@code actionName}에
 * 해당하는 컬럼이 없다. 그래서 짧은 분류값인 {@code reason} 컬럼을 {@code actionName}에,
 * 긴 설명인 {@code detailReason} 컬럼을 응답의 {@code reason}에 매핑한다.
 *
 * <p>지금은 {@code action_history}에 실제로 행을 써넣는 곳이 아직 하나도 없어서
 * {@code historyList}가 항상 빈 배열로 나가지만, 이건 데이터가 없어서지 조회 로직이 비어서가 아니다 —
 * 나중에 어떤 경로로든 행이 쌓이면 그대로 나간다.
 */
@JsonPropertyOrder({
	"reviewId", "status", "schoolName", "hasBadWordFlag", "rating",
	"createdAt", "reportCount", "content", "images",
	"userId", "nickname", "email", "reportList", "historyList"
})
public record ReviewDetailResponse(
	Long reviewId,
	ReviewStatus status,
	String schoolName,
	boolean hasBadWordFlag,
	Integer rating,
	@JsonFormat(pattern = "yyyy-MM-dd") LocalDateTime createdAt,
	long reportCount,
	String content,
	List<String> images,
	Long userId,
	String nickname,
	String email,
	List<ReportItem> reportList,
	List<HistoryItem> historyList
) {

	public static ReviewDetailResponse of(
		Review review,
		boolean hasBadWordFlag,
		List<String> images,
		List<Report> reports,
		List<ActionHistory> actionHistories
	) {
		User user = review.getUser();

		return new ReviewDetailResponse(
			review.getId(),
			review.getStatus(),
			user.getUniversity().getName(),
			hasBadWordFlag,
			review.getRating(),
			review.getCreatedAt(),
			reports.size(),
			review.getContent(),
			images,
			user.getId(),
			user.getNickname(),
			maskEmail(user.getUniversityEmail()),
			reports.stream().map(ReportItem::from).toList(),
			actionHistories.stream().map(HistoryItem::from).toList()
		);
	}

	/** 로컬파트 앞 3자만 남기고 나머지를 마스킹한다. {@code university_email}은 nullable이라 null이면 그대로 null. */
	private static String maskEmail(String email) {
		if (email == null) {
			return null;
		}

		int atIndex = email.indexOf('@');
		if (atIndex <= 0) {
			return email;
		}

		String localPart = email.substring(0, atIndex);
		String domain = email.substring(atIndex);
		int visibleLength = Math.min(3, localPart.length());

		return localPart.substring(0, visibleLength) + "*".repeat(localPart.length() - visibleLength) + domain;
	}

	public record ReportItem(
		String reportReason,
		Long reporterId,
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime reportedAt
	) {

		public static ReportItem from(Report report) {
			User reporter = report.getUser();

			return new ReportItem(
				report.getReason(),
				reporter == null ? null : reporter.getId(),
				report.getCreatedAt()
			);
		}
	}

	public record HistoryItem(
		String actionName,
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime actionAt,
		String handler,
		String reason
	) {

		public static HistoryItem from(ActionHistory actionHistory) {
			return new HistoryItem(
				actionHistory.getReason(),
				actionHistory.getCreatedAt(),
				actionHistory.getAdmin().getUsername(),
				actionHistory.getDetailReason()
			);
		}
	}
}
