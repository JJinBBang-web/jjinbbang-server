package com.jjinbbang.server.admin.moderation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.jjinbbang.server.admin.moderation.dto.response.ProhibitedWordListResponse;
import com.jjinbbang.server.admin.moderation.dto.response.ProhibitedWordResponse;
import com.jjinbbang.server.admin.moderation.exception.ModerationErrorCode;
import com.jjinbbang.server.admin.moderation.service.ProhibitedWordService;
import com.jjinbbang.server.global.error.GlobalExceptionHandler;
import com.jjinbbang.server.global.paging.PageInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProhibitedWordController")
class ProhibitedWordControllerTest {

	@Mock
	private ProhibitedWordService prohibitedWordService;

	@InjectMocks
	private ProhibitedWordController prohibitedWordController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(prohibitedWordController)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("등록에 성공하면 201 이고 본문의 code 도 201 이다")
	void 등록_성공은_201() throws Exception {
		// given
		given(prohibitedWordService.register(any()))
			.willReturn(new ProhibitedWordResponse(1L, "욕설", true, LocalDateTime.of(2026, 8, 2, 14, 30)));

		// when & then
		mockMvc.perform(post("/api/admin/prohibited-words")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"word\":\"욕설\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value(201))
			.andExpect(jsonPath("$.message").value("금칙어 등록 성공"))
			.andExpect(jsonPath("$.data.wordId").value(1))
			.andExpect(jsonPath("$.data.word").value("욕설"))
			.andExpect(jsonPath("$.data.enabled").value(true));
	}

	@Test
	@DisplayName("금칙어가 비어 있으면 400 이고 어느 필드인지 알려준다")
	void 금칙어가_비면_400() throws Exception {
		mockMvc.perform(post("/api/admin/prohibited-words")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"word\":\"  \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.errors[0].field").value("word"))
			.andExpect(jsonPath("$.errors[0].message").value("금칙어는 필수입니다."));
	}

	@Test
	@DisplayName("이미 등록된 금칙어면 409 이다")
	void 중복_등록은_409() throws Exception {
		// given
		given(prohibitedWordService.register(any()))
			.willThrow(ModerationErrorCode.DUPLICATE_PROHIBITED_WORD.exception("욕설"));

		// when & then
		mockMvc.perform(post("/api/admin/prohibited-words")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"word\":\"욕설\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("DUPLICATE_PROHIBITED_WORD"))
			.andExpect(jsonPath("$.message").value("이미 등록된 금칙어입니다. (욕설)"));
	}

	@Test
	@DisplayName("목록 조회는 prohibitedWordList 와 pageInfo 로 나간다")
	void 목록_응답_형태() throws Exception {
		// given
		given(prohibitedWordService.findAll(any(), any())).willReturn(listResponse());

		// when & then
		mockMvc.perform(get("/api/admin/prohibited-words"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("금칙어 사전 조회 성공"))
			.andExpect(jsonPath("$.data.prohibitedWordList[0].wordId").value(1))
			.andExpect(jsonPath("$.data.prohibitedWordList[0].enabled").value(true))
			.andExpect(jsonPath("$.data.pageInfo.page").value(0))
			.andExpect(jsonPath("$.data.pageInfo.size").value(20))
			.andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
			.andExpect(jsonPath("$.data.pageInfo.totalPages").value(1));
	}

	@Test
	@DisplayName("페이징을 안 주면 0 페이지 20건, 최신순으로 조회한다")
	void 페이징_기본값() throws Exception {
		// given
		given(prohibitedWordService.findAll(any(), any())).willReturn(listResponse());

		// when
		mockMvc.perform(get("/api/admin/prohibited-words")).andExpect(status().isOk());

		// then
		Pageable pageable = capturedPageable();
		assertThat(pageable.getPageNumber()).isZero();
		assertThat(pageable.getPageSize()).isEqualTo(20);
		assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
	}

	@Test
	@DisplayName("음수 페이지와 과도한 size 는 500 이 아니라 정상 범위로 눌린다")
	void 범위를_벗어난_페이징은_눌린다() throws Exception {
		// given
		given(prohibitedWordService.findAll(any(), any())).willReturn(listResponse());

		// when
		mockMvc.perform(get("/api/admin/prohibited-words")
				.param("page", "-5")
				.param("size", "100000"))
			.andExpect(status().isOk());

		// then
		Pageable pageable = capturedPageable();
		assertThat(pageable.getPageNumber()).isZero();
		assertThat(pageable.getPageSize()).isEqualTo(100);
	}

	@Test
	@DisplayName("enabled 를 주면 그대로 서비스에 전달된다")
	void 활성_필터가_전달된다() throws Exception {
		// given
		given(prohibitedWordService.findAll(any(), any())).willReturn(listResponse());

		// when
		mockMvc.perform(get("/api/admin/prohibited-words").param("enabled", "false"))
			.andExpect(status().isOk());

		// then
		then(prohibitedWordService).should().findAll(eq(false), any());
	}

	@Test
	@DisplayName("활성 여부를 바꾸면 200 이고 바뀐 상태를 돌려준다")
	void 상태_변경은_200() throws Exception {
		// given
		given(prohibitedWordService.changeEnabled(1L, false))
			.willReturn(new ProhibitedWordResponse(1L, "욕설", false, LocalDateTime.of(2026, 8, 2, 14, 30)));

		// when & then
		mockMvc.perform(patch("/api/admin/prohibited-words/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"enabled\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("금칙어 상태 변경 성공"))
			.andExpect(jsonPath("$.data.enabled").value(false));
	}

	@Test
	@DisplayName("enabled 를 빼면 400 이다 — 빠진 값이 조용히 false 가 되지 않는다")
	void enabled_누락은_400() throws Exception {
		mockMvc.perform(patch("/api/admin/prohibited-words/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.errors[0].field").value("enabled"));
	}

	@Test
	@DisplayName("삭제에 성공하면 200 이고 data 는 비어 있다")
	void 삭제_성공은_200() throws Exception {
		mockMvc.perform(delete("/api/admin/prohibited-words/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("금칙어 삭제 성공"))
			.andExpect(jsonPath("$.data").isEmpty());

		then(prohibitedWordService).should().delete(1L);
	}

	@Test
	@DisplayName("없는 금칙어를 삭제하면 404")
	void 없는_금칙어_삭제는_404() throws Exception {
		// given
		willThrow(ModerationErrorCode.PROHIBITED_WORD_NOT_FOUND.exception())
			.given(prohibitedWordService).delete(999L);

		// when & then
		mockMvc.perform(delete("/api/admin/prohibited-words/999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("PROHIBITED_WORD_NOT_FOUND"))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	@DisplayName("wordId 가 숫자가 아니면 400 INVALID_TYPE 이고 어느 값인지 알려준다")
	void wordId_타입_불일치는_400() throws Exception {
		mockMvc.perform(delete("/api/admin/prohibited-words/abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_TYPE"))
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다. (wordId)"));
	}

	private Pageable capturedPageable() {
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		then(prohibitedWordService).should().findAll(any(), captor.capture());
		return captor.getValue();
	}

	private ProhibitedWordListResponse listResponse() {
		return new ProhibitedWordListResponse(
			List.of(new ProhibitedWordResponse(1L, "욕설", true, LocalDateTime.of(2026, 8, 2, 14, 30))),
			new PageInfo(0, 20, 1, 1)
		);
	}
}
