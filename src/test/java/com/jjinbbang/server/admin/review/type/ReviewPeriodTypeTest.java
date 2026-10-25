package com.jjinbbang.server.admin.review.type;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewPeriodType")
class ReviewPeriodTypeTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 12, 0);

	@Test
	@DisplayName("LAST_7_DAYS 는 7일 전 시각을 돌려준다")
	void LAST_7_DAYS_는_7일_전이다() {
		assertThat(ReviewPeriodType.LAST_7_DAYS.resolveCreatedAfter(NOW))
			.isEqualTo(NOW.minusDays(7));
	}

	@Test
	@DisplayName("LAST_30_DAYS 는 30일 전 시각을 돌려준다")
	void LAST_30_DAYS_는_30일_전이다() {
		assertThat(ReviewPeriodType.LAST_30_DAYS.resolveCreatedAfter(NOW))
			.isEqualTo(NOW.minusDays(30));
	}

	@Test
	@DisplayName("LAST_1_YEAR 는 1년 전 시각을 돌려준다")
	void LAST_1_YEAR_는_1년_전이다() {
		assertThat(ReviewPeriodType.LAST_1_YEAR.resolveCreatedAfter(NOW))
			.isEqualTo(NOW.minusYears(1));
	}
}
