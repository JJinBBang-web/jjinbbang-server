package com.jjinbbang.server.global.error;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jjinbbang.server.global.template.ResTemplate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("도메인 ErrorCode 의 상태와 코드값이 그대로 응답으로 나간다")
	void 도메인_에러코드가_응답이_된다() throws Exception {
		mockMvc.perform(get("/test/not-found"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(404))
			.andExpect(jsonPath("$.errorCode").value("SAMPLE_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("샘플을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("같은 사유는 항상 같은 상태로 나간다 — 호출부가 상태를 고르지 않는다")
	void 상태는_코드값이_정한다() throws Exception {
		mockMvc.perform(get("/test/access-denied"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("SAMPLE_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("메시지 인자가 채워진 상태로 응답에 담긴다")
	void 메시지_인자가_채워진다() throws Exception {
		mockMvc.perform(get("/test/not-found/42"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("샘플(42)을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("검증 실패가 아니면 errors 필드가 붙지 않는다")
	void 검증_실패가_아니면_errors_가_없다() throws Exception {
		mockMvc.perform(get("/test/not-found"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	@DisplayName("검증 실패는 400 이고 필드별로 전부 내려준다")
	void 검증_실패는_필드별로_내려준다() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"\",\"content\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.errors.length()").value(2))
			.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("title", "content")));
	}

	@Test
	@DisplayName("검증을 통과하면 ResTemplate 형식으로 나간다")
	void 검증을_통과하면_성공_응답이_나간다() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"제목\",\"content\":\"내용\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("등록 성공"))
			.andExpect(jsonPath("$.data").value("제목"));
	}

	@Test
	@DisplayName("본문이 깨진 JSON 이면 400 INVALID_INPUT")
	void 본문_파싱_실패는_400() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\": "))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
	}

	@Test
	@DisplayName("경로 변수 타입이 맞지 않으면 400 INVALID_TYPE 이고 어느 값인지 알려준다")
	void 타입_불일치는_400() throws Exception {
		mockMvc.perform(get("/test/not-found/abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_TYPE"))
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다. (id)"));
	}

	@Test
	@DisplayName("지원하지 않는 요청 방식은 405")
	void 지원하지_않는_메서드는_405() throws Exception {
		mockMvc.perform(post("/test/not-found"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	@DisplayName("예상하지 못한 예외는 500 이고 내부 사정을 응답에 노출하지 않는다")
	void 예상하지_못한_예외는_500() throws Exception {
		mockMvc.perform(get("/test/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
	}

	@RestController
	@RequestMapping("/test")
	static class SampleController {

		@GetMapping("/not-found")
		public void notFound() {
			throw TestErrorCode.SAMPLE_NOT_FOUND.exception();
		}

		@GetMapping("/access-denied")
		public void accessDenied() {
			throw TestErrorCode.SAMPLE_ACCESS_DENIED.exception();
		}

		@GetMapping("/not-found/{id}")
		public void notFoundWithId(@PathVariable("id") Long id) {
			throw TestErrorCode.SAMPLE_NOT_FOUND_WITH_ID.exception(id);
		}

		@PostMapping("/validate")
		public ResTemplate<String> validate(@Valid @RequestBody SampleRequest request) {
			return new ResTemplate<>(org.springframework.http.HttpStatus.OK, "등록 성공", request.title());
		}

		@GetMapping("/boom")
		public void boom() {
			throw new IllegalStateException("데이터베이스 커넥션 정보 같은 내부 사정");
		}
	}

	record SampleRequest(
		@NotBlank(message = "제목은 필수입니다.") String title,
		@NotBlank(message = "내용은 필수입니다.") String content
	) {
	}
}
