package com.jmarket.support.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum SupportMinorCategory {
    CANCEL_REQUEST("취소요청", SupportMajorCategory.TRADE_CANCEL_OR_END),
    END_REQUEST("종료요청", SupportMajorCategory.TRADE_CANCEL_OR_END),
    TRADE_CANCEL_CASE("거래취소건", SupportMajorCategory.TRADE_ACCIDENT),
    TRADE_END_CASE("거래종료건", SupportMajorCategory.TRADE_ACCIDENT),
    ITEM_DURING_TRADE("거래중물품", SupportMajorCategory.TRADE_ACCIDENT),
    OTHER_ITEM_CONSULT("기타물품상담", SupportMajorCategory.TRADE_ACCIDENT),
    LOGIN_ISSUE("로그인문의", SupportMajorCategory.USAGE),
    CHARGE_DEPOSIT_ISSUE("충전/입금문의", SupportMajorCategory.USAGE),
    OTHER_USAGE("기타문의", SupportMajorCategory.USAGE);

    private final String label;
    private final SupportMajorCategory majorCategory;

    SupportMinorCategory(String label, SupportMajorCategory majorCategory) {
        this.label = label;
        this.majorCategory = majorCategory;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    public SupportMajorCategory getMajorCategory() {
        return majorCategory;
    }

    @JsonCreator
    public static SupportMinorCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(category -> category.name().equalsIgnoreCase(value) || category.label.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown minor category: " + value));
    }
}
