package com.jmarket.admin.domain;

public enum UserRestrictionType {
    PRODUCT_CREATE("상품 등록 제한"),
    AUCTION_CREATE("경매 등록 제한"),
    AUCTION_BID("입찰 제한");

    private final String label;

    UserRestrictionType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
