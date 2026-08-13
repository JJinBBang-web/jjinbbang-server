package com.jjinbbang.server.admin.moderation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjinbbang.server.admin.moderation.dto.response.ReportDismissResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ReportListResponse;
import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.exception.ModerationErrorCode;
import com.jjinbbang.server.admin.moderation.repository.ReportRepository;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

	private final ReportRepository reportRepository;

	/** {@code status}가 {@code null}이면 상태를 가리지 않고 전부 가져온다. */
	public ReportListResponse findAll(ReportStatus status, Pageable pageable) {
		Page<Report> page = status == null
			? reportRepository.findPageWithDetails(pageable)
			: reportRepository.findPageByStatusWithDetails(status, pageable);

		return ReportListResponse.from(page);
	}

	/**
	 * 신고를 기각한다. 대상 리뷰는 건드리지 않는다.
	 *
	 * <p>이미 승인·기각된 신고를 다시 처리하면 409다 — 두 관리자가 같은 신고를 동시에 열어놓고
	 * 서로 다른 판단을 눌렀을 때 나중 것이 조용히 이기면 안 된다.
	 */
	@Transactional
	public ReportDismissResponse dismiss(Long reportId) {
		Report report = reportRepository.findById(reportId)
			.orElseThrow(ModerationErrorCode.REPORT_NOT_FOUND::exception);

		if (report.isHandled()) {
			throw ModerationErrorCode.REPORT_ALREADY_HANDLED.exception();
		}

		report.dismiss();

		return ReportDismissResponse.from(report);
	}
}
