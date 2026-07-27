package com.jjinbbang.server.domain.review.type;

import jakarta.persistence.EnumeratedValue;

public enum ContractType {
	MONTHLY_RENT("월세"),
	DEPOSIT_RENT("전세");

	@EnumeratedValue
	private final String databaseValue;

	ContractType(String databaseValue) {
		this.databaseValue = databaseValue;
	}

	public String databaseValue() {
		return databaseValue;
	}

}
