package com.jjinbbang.server.global.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("ResTemplate")
class ResTemplateTest {

	// Boot 4 는 Jackson 3 (tools.jackson) 이다. jackson-annotations 만 2.x 가 남아
	// @JsonPropertyOrder 등은 com.fasterxml.jackson.annotation 그대로 쓴다.
	private final JsonMapper objectMapper = JsonMapper.builder().build();

	@Test
	@DisplayName("code · message · data 순서로 직렬화된다")
	void 필드_순서가_고정된다() throws Exception {
		// given
		ResTemplate<String> response = new ResTemplate<>(HttpStatus.OK, "조회 성공", "값");

		// when
		String json = objectMapper.writeValueAsString(response);

		// then
		assertThat(json).isEqualTo("{\"code\":200,\"message\":\"조회 성공\",\"data\":\"값\"}");
	}

	@Test
	@DisplayName("data 가 없으면 null 로 나간다")
	void data_가_없으면_null() throws Exception {
		// given
		ResTemplate<Void> response = new ResTemplate<>(HttpStatus.OK, "삭제 성공");

		// when
		String json = objectMapper.writeValueAsString(response);

		// then
		assertThat(json).isEqualTo("{\"code\":200,\"message\":\"삭제 성공\",\"data\":null}");
	}

	@Test
	@DisplayName("code 는 HttpStatus 값을 그대로 담는다")
	void code_는_HttpStatus_값이다() {
		// given
		ResTemplate<Void> response = new ResTemplate<>(HttpStatus.CREATED, "생성 성공");

		// when & then
		assertThat(response.getCode()).isEqualTo(201);
		assertThat(response.getMessage()).isEqualTo("생성 성공");
		assertThat(response.getData()).isNull();
	}
}
