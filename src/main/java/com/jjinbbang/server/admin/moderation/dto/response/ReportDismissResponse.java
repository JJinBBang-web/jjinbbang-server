package com.jjinbbang.server.admin.moderation.dto.response;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;

/** 기각 처리 결과. 바뀐 상태를 그대로 돌려줘 어드민 화면이 다시 조회하지 않아도 되게 한다. */
public record ReportDismissResponse(
	Long reportId,
	ReportStatus status
) {

	public static ReportDismissResponse from(Report report) {
		return new ReportDismissResponse(report.getId(), report.getStatus());
	}
}
