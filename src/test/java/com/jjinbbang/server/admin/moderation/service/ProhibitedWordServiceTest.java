package com.jjinbbang.server.admin.moderation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.jjinbbang.server.admin.moderation.dto.request.ProhibitedWordCreateRequest;
import com.jjinbbang.server.admin.moderation.dto.response.ProhibitedWordListResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ProhibitedWordResponse;
import com.jjinbbang.server.admin.moderation.entity.ProhibitedWord;
import com.jjinbbang.server.admin.moderation.exception.ModerationErrorCode;
import com.jjinbbang.server.admin.moderation.repository.ProhibitedWordRepository;
import com.jjinbbang.server.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProhibitedWordService")
class ProhibitedWordServiceTest {

	private static final Pageable PAGEABLE = PageRequest.of(0, 20);

	@Mock
	private ProhibitedWordRepository prohibitedWordRepository;

	@InjectMocks
	private ProhibitedWordService prohibitedWordService;

	@Test
	@DisplayName("금칙어를 등록하면 활성 상태로 저장된다")
	void 등록하면_활성_상태로_저장된다() {
		// given
		given(prohibitedWordRepository.existsByWordAndDeletedAtIsNull("욕설")).willReturn(false);
		given(prohibitedWordRepository.save(any(ProhibitedWord.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		ProhibitedWordResponse response =
			prohibitedWordService.register(new ProhibitedWordCreateRequest("욕설"));

		// then
		assertThat(response.word()).isEqualTo("욕설");
		assertThat(response.enabled()).isTrue();
	}

	@Test
	@DisplayName("앞뒤 공백을 잘라내고 저장한다 — 공백만 다른 값이 중복 검사를 빠져나가지 않게")
	void 앞뒤_공백을_잘라내고_저장한다() {
		// given
		given(prohibitedWordRepository.existsByWordAndDeletedAtIsNull("욕설")).willReturn(false);
		given(prohibitedWordRepository.save(any(ProhibitedWord.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		prohibitedWordService.register(new ProhibitedWordCreateRequest("  욕설  "));

		// then
		ArgumentCaptor<ProhibitedWord> captor = ArgumentCaptor.forClass(ProhibitedWord.class);
		then(prohibitedWordRepository).should().save(captor.capture());
		assertThat(captor.getValue().getWord()).isEqualTo("욕설");
	}

	@Test
	@DisplayName("이미 등록된 금칙어면 409 이고 저장하지 않는다")
	void 중복_금칙어는_저장하지_않는다() {
		// given
		given(prohibitedWordRepository.existsByWordAndDeletedAtIsNull("욕설")).willReturn(true);

		// when & then
		assertThatThrownBy(() -> prohibitedWordService.register(new ProhibitedWordCreateRequest("욕설")))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ModerationErrorCode.DUPLICATE_PROHIBITED_WORD);

		then(prohibitedWordRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("활성 필터가 없으면 활성·비활성을 가리지 않고 조회한다")
	void 필터가_없으면_전부_조회한다() {
		// given
		given(prohibitedWordRepository.findAllByDeletedAtIsNull(PAGEABLE))
			.willReturn(new PageImpl<>(List.of(prohibitedWord(1L, "욕설")), PAGEABLE, 1));

		// when
		ProhibitedWordListResponse response = prohibitedWordService.findAll(null, PAGEABLE);

		// then
		assertThat(response.prohibitedWordList()).hasSize(1);
		assertThat(response.pageInfo().totalElements()).isEqualTo(1);
		then(prohibitedWordRepository).should(never()).findAllByEnabledAndDeletedAtIsNull(any(), any());
	}

	@Test
	@DisplayName("활성 필터가 있으면 해당 상태만 조회한다")
	void 필터가_있으면_해당_상태만_조회한다() {
		// given
		given(prohibitedWordRepository.findAllByEnabledAndDeletedAtIsNull(true, PAGEABLE))
			.willReturn(new PageImpl<>(List.of(prohibitedWord(1L, "욕설")), PAGEABLE, 1));

		// when
		prohibitedWordService.findAll(true, PAGEABLE);

		// then
		then(prohibitedWordRepository).should().findAllByEnabledAndDeletedAtIsNull(true, PAGEABLE);
		then(prohibitedWordRepository).should(never()).findAllByDeletedAtIsNull(any());
	}

	@Test
	@DisplayName("비활성화하면 enabled 가 false 가 된다")
	void 비활성화하면_enabled_가_false_가_된다() {
		// given
		ProhibitedWord prohibitedWord = prohibitedWord(1L, "욕설");
		given(prohibitedWordRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(prohibitedWord));

		// when
		ProhibitedWordResponse response = prohibitedWordService.changeEnabled(1L, false);

		// then
		assertThat(prohibitedWord.getEnabled()).isFalse();
		assertThat(response.enabled()).isFalse();
	}

	@Test
	@DisplayName("없는 금칙어의 상태를 바꾸면 404")
	void 없는_금칙어_상태_변경은_404() {
		// given
		given(prohibitedWordRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prohibitedWordService.changeEnabled(999L, false))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ModerationErrorCode.PROHIBITED_WORD_NOT_FOUND);
	}

	@Test
	@DisplayName("삭제는 행을 지우지 않고 deleted_at 을 채운다")
	void 삭제는_소프트_삭제다() {
		// given
		ProhibitedWord prohibitedWord = prohibitedWord(1L, "욕설");
		given(prohibitedWordRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(prohibitedWord));

		// when
		prohibitedWordService.delete(1L);

		// then
		assertThat(prohibitedWord.getDeletedAt()).isNotNull();
		assertThat(prohibitedWord.isDeleted()).isTrue();
		then(prohibitedWordRepository).should(never()).delete(any());
	}

	@Test
	@DisplayName("이미 삭제된 금칙어는 조회 대상이 아니라 404")
	void 이미_삭제된_금칙어는_404() {
		// given
		given(prohibitedWordRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prohibitedWordService.delete(1L))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ModerationErrorCode.PROHIBITED_WORD_NOT_FOUND);
	}

	private ProhibitedWord prohibitedWord(Long id, String word) {
		ProhibitedWord prohibitedWord = ProhibitedWord.create(word);
		ReflectionTestUtils.setField(prohibitedWord, "id", id);
		return prohibitedWord;
	}
}
