package com.jjinbbang.server.admin.moderation.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.jjinbbang.server.admin.moderation.entity.ProhibitedWord;
import com.jjinbbang.server.global.paging.PageInfo;

public record ProhibitedWordListResponse(
	List<ProhibitedWordResponse> prohibitedWordList,
	PageInfo pageInfo
) {

	public static ProhibitedWordListResponse from(Page<ProhibitedWord> page) {
		return new ProhibitedWordListResponse(
			page.getContent().stream().map(ProhibitedWordResponse::from).toList(),
			PageInfo.from(page)
		);
	}
}
