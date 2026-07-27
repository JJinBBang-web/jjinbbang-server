package com.jjinbbang.server.domain.review.entity;

import java.util.Arrays;

public enum ContractType {
	MONTHLY_RENT("월세"),
	DEPOSIT_RENT("전세");

	private final String databaseValue;

	ContractType(String databaseValue) {
		this.databaseValue = databaseValue;
	}

	public String databaseValue() {
		return databaseValue;
	}

	public static ContractType fromDatabaseValue(String value) {
		return Arrays.stream(values())
			.filter(type -> type.databaseValue.equals(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown contract type: " + value));
	}
}
