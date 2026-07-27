package com.jjinbbang.server.domain.content.entity;

import java.util.Arrays;

public enum ContentCategory {
	REAL_ESTATE("부동산"),
	TIPS("자취꿀팁"),
	CAMPUS_LIFE("대학생활"),
	MOVING("이사관련");

	private final String databaseValue;

	ContentCategory(String databaseValue) {
		this.databaseValue = databaseValue;
	}

	public String databaseValue() {
		return databaseValue;
	}

	public static ContentCategory fromDatabaseValue(String value) {
		return Arrays.stream(values())
			.filter(category -> category.databaseValue.equals(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown content category: " + value));
	}
}
