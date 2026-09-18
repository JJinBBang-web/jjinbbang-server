package com.jjinbbang.server.admin.verification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 합격증명서 반려 요청. */
public record AdmissionCertificateRejectRequest(

	@NotBlank(message = "반려 사유는 필수입니다.")
	@Size(max = 255, message = "반려 사유는 255자를 넘을 수 없습니다.")
	String rejectReason
) {
}
