package com.jjinbbang.server.admin.review.dto.response;

public record ReviewMaskPreviewResponse(
	String maskedContent
) {

	public static ReviewMaskPreviewResponse from(String maskedContent) {
		return new ReviewMaskPreviewResponse(maskedContent);
	}
}
