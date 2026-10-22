package com.jjinbbang.server.admin.moderation.dto.response;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.global.paging.PageInfo;

/**
 * 신고 목록. 페이지 단위는 신고가 아니라 <b>신고된 리뷰</b>다 — {@code pageInfo}도 리뷰 수 기준이다.
 *
 * <p>신고 단위로 자르면 같은 리뷰의 신고가 페이지 경계에서 갈라져, 화면이 리뷰별로 묶을 때
 * 같은 리뷰가 두 페이지에 나오고 "신고 N건"이 실제보다 적게 보인다.
 */
public record ReportReviewListResponse(
	List<ReportReviewResponse> reportReviewList,
	PageInfo pageInfo
) {

	/**
	 * {@code reviewIds} 페이지의 순서대로 행을 만든다.
	 *
	 * <p>페이지를 고른 뒤 신고를 모으기 전에 다른 관리자가 그 리뷰의 신고를 전부 처리하면,
	 * 상태 필터에 걸려 신고가 한 건도 오지 않는다. 그런 리뷰는 빈 행으로 내리지 않고 뺀다.
	 */
	public static ReportReviewListResponse of(Page<Long> reviewIds, Map<Long, List<Report>> reportsByReviewId) {
		return new ReportReviewListResponse(
			reviewIds.getContent().stream()
				.map(reportsByReviewId::get)
				.filter(Objects::nonNull)
				.map(ReportReviewResponse::from)
				.toList(),
			PageInfo.from(reviewIds)
		);
	}
}
