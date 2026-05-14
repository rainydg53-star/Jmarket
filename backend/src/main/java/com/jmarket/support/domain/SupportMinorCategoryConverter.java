package com.jmarket.support.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SupportMinorCategoryConverter implements AttributeConverter<SupportMinorCategory, String> {

    @Override
    public String convertToDatabaseColumn(SupportMinorCategory attribute) {
        return attribute != null ? attribute.getLabel() : null;
    }

    @Override
    public SupportMinorCategory convertToEntityAttribute(String dbData) {
        return dbData != null ? SupportMinorCategory.fromValue(dbData) : null;
    }
}
