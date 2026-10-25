package com.jjinbbang.server.admin.moderation.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jjinbbang.server.admin.moderation.dto.response.ReportDismissResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ReportReviewListResponse;
import com.jjinbbang.server.admin.moderation.service.ReportService;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;
import com.jjinbbang.server.global.paging.PageRequests;
import com.jjinbbang.server.global.template.ResTemplate;

import lombok.RequiredArgsConstructor;

/**
 * 리뷰 신고 관리. 어드민 웹의 "신고 관리" 화면이 호출한다.
 *
 * <p>{@code status}는 {@link ReportStatus} 상수 이름으로 받는다. enum 밖의 값이 오면
 * {@code MethodArgumentTypeMismatchException}으로 걸려 400 {@code INVALID_TYPE}이 된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
public class ReportController {

	private final ReportService reportService;

	/**
	 * 신고된 리뷰 단위로 페이징한다 — {@code size}는 리뷰 개수다. 리뷰 순서는 가장 최근 신고일 순으로
	 * 서비스가 정하므로 여기서 정렬을 넘기지 않는다.
	 */
	@GetMapping
	public ResTemplate<ReportReviewListResponse> findAll(
		@RequestParam(name = "status", required = false) ReportStatus status,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = PageRequests.DEFAULT_SIZE_PARAM) int size
	) {
		ReportReviewListResponse response = reportService.findAll(status, PageRequests.of(page, size, Sort.unsorted()));

		return new ResTemplate<>(HttpStatus.OK, "신고 목록 조회 성공", response);
	}

	@PostMapping("/dismiss/{reportId}")
	public ResTemplate<ReportDismissResponse> dismiss(@PathVariable("reportId") Long reportId) {
		return new ResTemplate<>(HttpStatus.OK, "신고 기각 성공", reportService.dismiss(reportId));
	}
}
