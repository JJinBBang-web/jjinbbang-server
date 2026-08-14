package com.jjinbbang.server.admin.review.type;

import java.time.LocalDateTime;

public enum ReviewPeriodType {
	LAST_7_DAYS,
	LAST_30_DAYS,
	LAST_1_YEAR;

	public LocalDateTime resolveCreatedAfter(LocalDateTime now) {
		return switch (this) {
			case LAST_7_DAYS -> now.minusDays(7);
			case LAST_30_DAYS -> now.minusDays(30);
			case LAST_1_YEAR -> now.minusYears(1);
		};
	}
}
