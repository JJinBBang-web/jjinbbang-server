package com.jjinbbang.server.admin.moderation.dto.response;

import java.util.List;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.domain.review.entity.Review;

/**
 * 신고 관리 화면의 한 행 — 신고된 리뷰 하나와 그 리뷰에 달린 신고 전부.
 *
 * <p>{@code schoolName}은 리뷰 관리 목록({@code admin.review})과 같이 <b>리뷰 작성자의 학교</b>다.
 * {@code reviews}에 제목 컬럼이 없어 {@code content}로 본문을 내려준다.
 *
 * <p>{@code totalReportCount}는 {@code subReports}의 크기다. 상태 필터가 걸려 있으면 그 상태의 신고만 센다.
 */
public record ReportReviewResponse(
	Long reviewId,
	String schoolName,
	String content,
	int totalReportCount,
	List<SubReportResponse> subReports
) {

	/** {@code reports}는 모두 같은 리뷰의 신고여야 하고, 비어 있으면 안 된다. */
	public static ReportReviewResponse from(List<Report> reports) {
		Review review = reports.getFirst().getReview();

		return new ReportReviewResponse(
			review.getId(),
			review.getUser().getUniversity().getName(),
			review.getContent(),
			reports.size(),
			reports.stream().map(SubReportResponse::from).toList()
		);
	}
}
