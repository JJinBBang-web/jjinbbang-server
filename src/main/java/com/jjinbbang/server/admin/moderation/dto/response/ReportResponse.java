package com.jjinbbang.server.admin.moderation.dto.response;

import java.time.LocalDateTime;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;
import com.jjinbbang.server.domain.user.entity.User;

/**
 * 신고 한 건.
 *
 * <p>스키마 때문에 명세서와 달라진 곳이 셋 있다.
 * <ul>
 *   <li>{@code reviews}에 제목 컬럼이 없어 {@code reviewContent}를 내려준다.</li>
 *   <li>{@code users}에 닉네임 컬럼이 없어 신고자는 ID와 소속 대학으로 식별한다.</li>
 *   <li>{@code reports}는 {@code created_at}만 가진다 — {@code updatedAt}은 내려줄 수 없다.</li>
 * </ul>
 *
 * <p>{@code reports.user_id}가 nullable이라 신고자 두 필드가 {@code null}로 나갈 수 있다.
 * 회원 탈퇴는 소프트 삭제라 그것만으로는 null이 되지 않는다 — 근거는 {@code ReportRepository} 참조.
 */
public record ReportResponse(
	Long reportId,
	Long reviewId,
	String reviewContent,
	Long reporterId,
	String reporterUniversity,
	String reason,
	ReportStatus status,
	LocalDateTime createdAt
) {

	public static ReportResponse from(Report report) {
		User reporter = report.getUser();

		return new ReportResponse(
			report.getId(),
			report.getReview().getId(),
			report.getReview().getContent(),
			reporter == null ? null : reporter.getId(),
			reporter == null ? null : reporter.getUniversity().getName(),
			report.getReason(),
			report.getStatus(),
			report.getCreatedAt()
		);
	}
}
