package com.jmarket.support.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum SupportMajorCategory {
    TRADE_CANCEL_OR_END("거래취소/종료"),
    TRADE_ACCIDENT("거래사고"),
    USAGE("이용관련");

    private final String label;

    SupportMajorCategory(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static SupportMajorCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(category -> category.name().equalsIgnoreCase(value) || category.label.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown major category: " + value));
    }
}
