package com.jjinbbang.server.admin.review.dto.request;

import java.util.List;

import com.jjinbbang.server.admin.review.type.ReviewMaskReason;

import jakarta.validation.constraints.NotEmpty;

public record ReviewMaskRequest(

	@NotEmpty(message = "조치 사유는 필수입니다.")
	List<ReviewMaskReason> reasons,

	String detailReason
) {
}
