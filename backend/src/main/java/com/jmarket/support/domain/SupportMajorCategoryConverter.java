package com.jmarket.support.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SupportMajorCategoryConverter implements AttributeConverter<SupportMajorCategory, String> {

    @Override
    public String convertToDatabaseColumn(SupportMajorCategory attribute) {
        return attribute != null ? attribute.getLabel() : null;
    }

    @Override
    public SupportMajorCategory convertToEntityAttribute(String dbData) {
        return dbData != null ? SupportMajorCategory.fromValue(dbData) : null;
    }
}
