package com.jmarket.product.domain;

public enum ProductCategory {
    DIGITAL_APPLIANCE("디지털/가전"),
    FASHION("패션/잡화"),
    BEAUTY_HEALTH("뷰티/헬스"),
    LIVING_INTERIOR("리빙/인테리어"),
    LUXURY_WATCH("명품/시계"),
    COLLECTIBLE_GOODS("수집품/굿즈"),
    SPORTS_LEISURE("스포츠/레저"),
    BOOK_TICKET_GOODS("도서/티켓/굿즈"),
    ETC("기타");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ProductCategory from(String value) {
        if (value == null || value.isBlank()) {
            return ETC;
        }

        String normalizedValue = value.trim();
        ProductCategory legacyCategory = fromLegacyValue(normalizedValue);
        if (legacyCategory != null) {
            return legacyCategory;
        }

        for (ProductCategory category : values()) {
            if (category.name().equalsIgnoreCase(normalizedValue) || category.label.equals(normalizedValue)) {
                return category;
            }
        }
        return ETC;
    }

    private static ProductCategory fromLegacyValue(String value) {
        return switch (value.toUpperCase()) {
            case "DIGITAL" -> DIGITAL_APPLIANCE;
            case "HOME" -> LIVING_INTERIOR;
            case "BEAUTY" -> BEAUTY_HEALTH;
            case "SPORTS" -> SPORTS_LEISURE;
            case "BOOKS" -> BOOK_TICKET_GOODS;
            default -> null;
        };
    }
}
