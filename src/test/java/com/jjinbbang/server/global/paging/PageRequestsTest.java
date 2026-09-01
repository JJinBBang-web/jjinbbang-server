package com.jjinbbang.server.global.paging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 목록 API 가 전부 이 유틸을 거치므로, 범위를 벗어난 값이 500 이 되지 않는다는 것을 여기서 못박는다.
 * 컨트롤러 슬라이스 테스트는 한 도메인만 확인한다.
 */
@DisplayName("PageRequests")
class PageRequestsTest {

	private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

	@Test
	@DisplayName("정상 범위의 값은 그대로 통과한다")
	void 정상_범위는_그대로_통과한다() {
		// when
		Pageable pageable = PageRequests.of(2, 50, LATEST_FIRST);

		// then
		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(50);
		assertThat(pageable.getSort()).isEqualTo(LATEST_FIRST);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, -5, Integer.MIN_VALUE})
	@DisplayName("음수 페이지는 0 으로 눌린다 — PageRequest.of 가 던지면 500 이 나간다")
	void 음수_페이지는_0_으로_눌린다(int page) {
		// when
		Pageable pageable = PageRequests.of(page, 20, LATEST_FIRST);

		// then
		assertThat(pageable.getPageNumber()).isZero();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1, Integer.MIN_VALUE})
	@DisplayName("1 미만의 size 는 기본값이 된다 — size=0 은 PageRequest.of 가 거부한다")
	void 크기가_1_미만이면_기본값이_된다(int size) {
		// when
		Pageable pageable = PageRequests.of(0, size, LATEST_FIRST);

		// then
		assertThat(pageable.getPageSize()).isEqualTo(PageRequests.DEFAULT_SIZE);
	}

	@ParameterizedTest
	@ValueSource(ints = {101, 1_000_000, Integer.MAX_VALUE})
	@DisplayName("상한을 넘는 size 는 MAX_SIZE 로 눌린다 — 한 번의 요청으로 어드민 API 가 멈추지 않게")
	void 상한을_넘는_크기는_MAX_SIZE_로_눌린다(int size) {
		// when
		Pageable pageable = PageRequests.of(0, size, LATEST_FIRST);

		// then
		assertThat(pageable.getPageSize()).isEqualTo(PageRequests.MAX_SIZE);
	}

	@Test
	@DisplayName("경계값 1 과 MAX_SIZE 는 눌리지 않는다")
	void 경계값은_눌리지_않는다() {
		// when & then
		assertThat(PageRequests.of(0, 1, LATEST_FIRST).getPageSize()).isEqualTo(1);
		assertThat(PageRequests.of(0, PageRequests.MAX_SIZE, LATEST_FIRST).getPageSize())
			.isEqualTo(PageRequests.MAX_SIZE);
	}

	@Test
	@DisplayName("쿼리 파라미터 기본값 상수는 DEFAULT_SIZE 와 같은 값이다")
	void 파라미터_기본값_상수는_DEFAULT_SIZE_와_같다() {
		// when & then — 컨트롤러의 defaultValue 와 클램핑 기준이 어긋나지 않게 한다
		assertThat(PageRequests.DEFAULT_SIZE_PARAM).isEqualTo(String.valueOf(PageRequests.DEFAULT_SIZE));
	}
}
