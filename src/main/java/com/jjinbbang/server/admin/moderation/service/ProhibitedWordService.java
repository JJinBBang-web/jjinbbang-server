package com.jjinbbang.server.admin.moderation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjinbbang.server.admin.moderation.dto.request.ProhibitedWordCreateRequest;
import com.jjinbbang.server.admin.moderation.dto.response.ProhibitedWordListResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ProhibitedWordResponse;
import com.jjinbbang.server.admin.moderation.entity.ProhibitedWord;
import com.jjinbbang.server.admin.moderation.exception.ModerationErrorCode;
import com.jjinbbang.server.admin.moderation.repository.ProhibitedWordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProhibitedWordService {

	private final ProhibitedWordRepository prohibitedWordRepository;

	/**
	 * 금칙어를 등록한다.
	 *
	 * <p>앞뒤 공백은 잘라내고 저장한다 — {@code "  욕설"}과 {@code "욕설"}이 다른 금칙어로 들어가면
	 * 중복 검사가 무력해지고 필터링도 새기 때문이다.
	 */
	@Transactional
	public ProhibitedWordResponse register(ProhibitedWordCreateRequest request) {
		String word = request.word().strip();

		if (prohibitedWordRepository.existsByWordAndDeletedAtIsNull(word)) {
			throw ModerationErrorCode.DUPLICATE_PROHIBITED_WORD.exception(word);
		}

		return ProhibitedWordResponse.from(prohibitedWordRepository.save(ProhibitedWord.create(word)));
	}

	/** {@code enabled}가 {@code null}이면 활성·비활성을 가리지 않고 전부 가져온다. */
	public ProhibitedWordListResponse findAll(Boolean enabled, Pageable pageable) {
		Page<ProhibitedWord> page = enabled == null
			? prohibitedWordRepository.findAllByDeletedAtIsNull(pageable)
			: prohibitedWordRepository.findAllByEnabledAndDeletedAtIsNull(enabled, pageable);

		return ProhibitedWordListResponse.from(page);
	}

	@Transactional
	public ProhibitedWordResponse changeEnabled(Long wordId, boolean enabled) {
		ProhibitedWord prohibitedWord = findActiveById(wordId);
		prohibitedWord.changeEnabled(enabled);

		return ProhibitedWordResponse.from(prohibitedWord);
	}

	/**
	 * 금칙어를 삭제한다. 행을 지우지 않고 {@code deleted_at}을 채운다 —
	 * {@code prohibited_word_flags}가 과거에 어떤 금칙어로 걸렸는지를 참조하고 있어 지우면 이력이 끊긴다.
	 */
	@Transactional
	public void delete(Long wordId) {
		findActiveById(wordId).delete();
	}

	private ProhibitedWord findActiveById(Long wordId) {
		return prohibitedWordRepository.findByIdAndDeletedAtIsNull(wordId)
			.orElseThrow(ModerationErrorCode.PROHIBITED_WORD_NOT_FOUND::exception);
	}
}
