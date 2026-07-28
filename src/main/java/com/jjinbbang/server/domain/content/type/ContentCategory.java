package com.jjinbbang.server.domain.content.type;

import jakarta.persistence.EnumeratedValue;

public enum ContentCategory {
	REAL_ESTATE("부동산"),
	TIPS("자취꿀팁"),
	CAMPUS_LIFE("대학생활"),
	MOVING("이사관련");

	@EnumeratedValue
	private final String databaseValue;

	ContentCategory(String databaseValue) {
		this.databaseValue = databaseValue;
	}

	public String databaseValue() {
		return databaseValue;
	}

}
