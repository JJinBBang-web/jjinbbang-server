package com.jjinbbang.server.admin.moderation.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jjinbbang.server.admin.moderation.dto.request.ProhibitedWordCreateRequest;
import com.jjinbbang.server.admin.moderation.dto.request.ProhibitedWordStatusUpdateRequest;
import com.jjinbbang.server.admin.moderation.dto.response.ProhibitedWordListResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ProhibitedWordResponse;
import com.jjinbbang.server.admin.moderation.service.ProhibitedWordService;
import com.jjinbbang.server.global.paging.PageRequests;
import com.jjinbbang.server.global.template.ResTemplate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 금칙어 사전 관리. 어드민 웹의 "금칙어 사전" 화면이 호출한다.
 *
 * <p>경로가 {@code prohibited-words}인 것은 테이블명({@code prohibited_words})을 따른 것이다.
 * 명세서에 있던 {@code bad-words} · {@code badword} 표기는 쓰지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/prohibited-words")
public class ProhibitedWordController {

	private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

	private final ProhibitedWordService prohibitedWordService;

	@PostMapping
	public ResponseEntity<ResTemplate<ProhibitedWordResponse>> register(
		@Valid @RequestBody ProhibitedWordCreateRequest request
	) {
		ProhibitedWordResponse response = prohibitedWordService.register(request);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(new ResTemplate<>(HttpStatus.CREATED, "금칙어 등록 성공", response));
	}

	@GetMapping
	public ResTemplate<ProhibitedWordListResponse> findAll(
		@RequestParam(name = "enabled", required = false) Boolean enabled,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = "20") int size
	) {
		ProhibitedWordListResponse response =
			prohibitedWordService.findAll(enabled, PageRequests.of(page, size, LATEST_FIRST));

		return new ResTemplate<>(HttpStatus.OK, "금칙어 사전 조회 성공", response);
	}

	@PatchMapping("/{wordId}")
	public ResTemplate<ProhibitedWordResponse> changeEnabled(
		@PathVariable("wordId") Long wordId,
		@Valid @RequestBody ProhibitedWordStatusUpdateRequest request
	) {
		ProhibitedWordResponse response = prohibitedWordService.changeEnabled(wordId, request.enabled());

		return new ResTemplate<>(HttpStatus.OK, "금칙어 상태 변경 성공", response);
	}

	@DeleteMapping("/{wordId}")
	public ResTemplate<Void> delete(@PathVariable("wordId") Long wordId) {
		prohibitedWordService.delete(wordId);

		return new ResTemplate<>(HttpStatus.OK, "금칙어 삭제 성공");
	}
}
