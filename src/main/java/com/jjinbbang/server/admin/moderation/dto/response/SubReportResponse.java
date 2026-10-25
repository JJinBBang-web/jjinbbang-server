package com.jjinbbang.server.admin.moderation.dto.response;

import java.time.LocalDateTime;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;
import com.jjinbbang.server.domain.user.entity.User;

/**
 * 리뷰 행 아래에 펼쳐지는 신고 한 건. 대상 리뷰 정보는 바깥 {@link ReportReviewResponse}가 들고 있다.
 *
 * <p>{@code reports.user_id}가 nullable이라 {@code reporterId}가 {@code null}로 나갈 수 있다.
 * {@code reports}는 {@code created_at}만 가지므로 {@code reportedAt}이 곧 신고 접수 시각이다.
 */
public record SubReportResponse(
	Long reportId,
	Long reporterId,
	String reason,
	ReportStatus status,
	LocalDateTime reportedAt
) {

	public static SubReportResponse from(Report report) {
		User reporter = report.getUser();

		return new SubReportResponse(
			report.getId(),
			reporter == null ? null : reporter.getId(),
			report.getReason(),
			report.getStatus(),
			report.getCreatedAt()
		);
	}
}
