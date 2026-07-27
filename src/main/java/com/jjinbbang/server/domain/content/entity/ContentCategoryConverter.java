package com.jjinbbang.server.domain.content.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ContentCategoryConverter implements AttributeConverter<ContentCategory, String> {

	@Override
	public String convertToDatabaseColumn(ContentCategory attribute) {
		return attribute == null ? null : attribute.databaseValue();
	}

	@Override
	public ContentCategory convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ContentCategory.fromDatabaseValue(dbData);
	}
}
