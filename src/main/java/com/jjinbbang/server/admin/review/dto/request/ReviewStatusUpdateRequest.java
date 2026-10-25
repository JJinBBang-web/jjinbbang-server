package com.jjinbbang.server.admin.review.dto.request;

import com.jjinbbang.server.domain.review.type.ReviewStatus;

import jakarta.validation.constraints.NotNull;

public record ReviewStatusUpdateRequest(

	@NotNull(message = "상태는 필수입니다.")
	ReviewStatus status
) {
}
