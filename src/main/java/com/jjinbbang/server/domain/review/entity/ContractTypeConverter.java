package com.jjinbbang.server.domain.review.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ContractTypeConverter implements AttributeConverter<ContractType, String> {

	@Override
	public String convertToDatabaseColumn(ContractType attribute) {
		return attribute == null ? null : attribute.databaseValue();
	}

	@Override
	public ContractType convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ContractType.fromDatabaseValue(dbData);
	}
}
