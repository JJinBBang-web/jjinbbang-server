package com.jjinbbang.server.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("ErrorCode")
class ErrorCodeTest {

	@Test
	@DisplayName("코드값은 enum 이름을 그대로 쓴다")
	void code_는_enum_이름과_같다() {
		// when & then
		assertThat(TestErrorCode.SAMPLE_NOT_FOUND.getCode()).isEqualTo("SAMPLE_NOT_FOUND");
	}

	@Test
	@DisplayName("exception() 은 코드값을 담은 BusinessException 을 만든다")
	void exception_은_코드값을_담는다() {
		// when
		BusinessException exception = TestErrorCode.SAMPLE_NOT_FOUND.exception();

		// then
		assertThat(exception.getErrorCode()).isEqualTo(TestErrorCode.SAMPLE_NOT_FOUND);
		assertThat(exception.getMessage()).isEqualTo("샘플을 찾을 수 없습니다.");
		assertThat(exception.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DisplayName("exception(args) 는 메시지의 형식 지정자를 채운다")
	void exception_은_메시지_인자를_채운다() {
		// when
		BusinessException exception = TestErrorCode.SAMPLE_NOT_FOUND_WITH_ID.exception(42L);

		// then
		assertThat(exception.getMessage()).isEqualTo("샘플(42)을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("던지면 BusinessException 으로 잡힌다 — 도메인마다 예외 클래스를 만들지 않는다")
	void 던지면_BusinessException_으로_잡힌다() {
		// when & then
		assertThatThrownBy(() -> {
			throw TestErrorCode.SAMPLE_ACCESS_DENIED.exception();
		})
			.isInstanceOf(BusinessException.class)
			.hasMessage("본인의 샘플만 수정할 수 있습니다.");
	}

	@Test
	@DisplayName("모든 코드값은 상태와 메시지를 갖는다")
	void 모든_코드값은_상태와_메시지를_갖는다() {
		// when & then
		for (GlobalErrorCode errorCode : GlobalErrorCode.values()) {
			assertThat(errorCode.getStatus()).isNotNull();
			assertThat(errorCode.getMessage()).isNotBlank();
			assertThat(errorCode.getCode()).isEqualTo(errorCode.name());
		}
	}
}
