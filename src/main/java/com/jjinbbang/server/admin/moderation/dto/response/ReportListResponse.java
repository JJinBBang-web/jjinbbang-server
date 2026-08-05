package com.jjinbbang.server.admin.moderation.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.global.paging.PageInfo;

public record ReportListResponse(
	List<ReportResponse> reportList,
	PageInfo pageInfo
) {

	public static ReportListResponse from(Page<Report> page) {
		return new ReportListResponse(
			page.getContent().stream().map(ReportResponse::from).toList(),
			PageInfo.from(page)
		);
	}
}
