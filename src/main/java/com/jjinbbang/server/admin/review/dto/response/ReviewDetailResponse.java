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
 * <p>{@code historyList}는 {@link ActionHistory}를 내려주는데, 이 엔티티엔 {@code actionNames}에
 * 해당하는 컬럼이 없다. 그래서 짧은 분류값인 {@code reason} 컬럼을 {@code actionNames}에,
 * 긴 설명인 {@code detailReason} 컬럼을 응답의 {@code reason}에 매핑한다. {@code reason}은 사유가
 * 여러 개면 쉼표로 합쳐 저장돼 있어 응답 시점에 다시 리스트로 쪼갠다.
 *
 * <p>리뷰 마스킹 확정({@code ReviewService.mask})이 {@code action_history}에 행을 쓰는 첫 경로다.
 * 신고 기각은 아직 사유를 받지 않아 이력을 남기지 않으므로, {@code historyList}는 마스킹 조치만
 * 채워진 채로 나갈 수 있다.
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
		List<String> images,
		List<Report> reports,
		List<ActionHistory> actionHistories
	) {
		User user = review.getUser();

		return new ReviewDetailResponse(
			review.getId(),
			review.getStatus(),
			user.getUniversity().getName(),
			review.isProhibitedWordFlag(),
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
		List<String> actionNames,
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime actionAt,
		String handler,
		String reason
	) {

		/** {@code actionHistory.reason}은 사유가 여러 개면 쉼표로 이어붙여 저장돼 있어 다시 리스트로 쪼갠다. */
		public static HistoryItem from(ActionHistory actionHistory) {
			return new HistoryItem(
				List.of(actionHistory.getReason().split(",")),
				actionHistory.getCreatedAt(),
				actionHistory.getAdmin().getUsername(),
				actionHistory.getDetailReason()
			);
		}
	}
}
