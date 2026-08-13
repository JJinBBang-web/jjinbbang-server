package com.jjinbbang.server.global.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 소프트 삭제 동작. {@code prohibited_words} 를 시작으로 소프트 삭제 테이블이 전부 이 메서드를 물려받으므로
 * 도메인 테스트가 아니라 여기서 확인한다.
 */
@DisplayName("SoftDeleteEntity")
class SoftDeleteEntityTest {

	@Test
	@DisplayName("삭제 전에는 deletedAt 이 비어 있다")
	void 삭제_전에는_deletedAt_이_비어_있다() {
		// given
		SoftDeleteSample sample = new SoftDeleteSample();

		// when & then
		assertThat(sample.getDeletedAt()).isNull();
		assertThat(sample.isDeleted()).isFalse();
	}

	@Test
	@DisplayName("삭제하면 deletedAt 이 채워진다")
	void 삭제하면_deletedAt_이_채워진다() {
		// given
		SoftDeleteSample sample = new SoftDeleteSample();
		LocalDateTime before = LocalDateTime.now();

		// when
		sample.delete();

		// then
		assertThat(sample.isDeleted()).isTrue();
		assertThat(sample.getDeletedAt()).isAfterOrEqualTo(before);
	}

	@Test
	@DisplayName("이미 삭제된 것을 다시 지워도 최초 삭제 시각을 덮어쓰지 않는다 — 감사 기록이 뒤로 밀리면 안 된다")
	void 재삭제는_최초_삭제_시각을_덮지_않는다() {
		// given
		SoftDeleteSample sample = new SoftDeleteSample();
		LocalDateTime firstDeletedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
		ReflectionTestUtils.setField(sample, "deletedAt", firstDeletedAt);

		// when
		sample.delete();

		// then
		assertThat(sample.getDeletedAt()).isEqualTo(firstDeletedAt);
	}

	/**
	 * 테스트 전용 구현체.
	 *
	 * <p><b>{@code @Entity} 를 붙이지 않는다.</b> 붙이면 Hibernate 엔티티 스캔에 잡혀
	 * {@code JjinbbangServerApplicationTests} 의 엔티티 개수 기대값이 어긋나고,
	 * 대응하는 테이블이 없어 {@code ddl-auto: validate} 도 실패한다.
	 * 여기서 확인하는 것은 {@code deletedAt} 을 다루는 순수 자바 로직이라 매핑이 필요 없다.
	 */
	static class SoftDeleteSample extends SoftDeleteEntity {
	}
}
