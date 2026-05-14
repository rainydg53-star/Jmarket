package com.jmarket.product.dto;

import com.jmarket.product.domain.Product;
import java.io.Serializable;
import java.time.LocalDateTime;

public record ProductSummaryResponse(
        Long id,
        String title,
        String category,
        String categoryLabel,
        Long price,
        boolean sold,
        String tradeStatus,
        String tradeStatusLabel,
        String thumbnailUrl,
        long viewCount,
        long favoriteCount,
        Long sellerId,
        String sellerNickname,
        long sellerReviewCount,
        double sellerAverageRating,
        double sellerMannerTemperature,
        LocalDateTime createdAt
) implements Serializable {
    public static ProductSummaryResponse from(Product product) {
        return from(product, null, product.getCategoryCode());
    }

    public static ProductSummaryResponse from(Product product, String thumbnailUrl) {
        return from(product, thumbnailUrl, product.getCategoryCode());
    }

    public static ProductSummaryResponse from(Product product, String thumbnailUrl, String categoryLabel) {
        return from(product, thumbnailUrl, categoryLabel, defaultTradeStatus(product), defaultTradeStatusLabel(product), 0L, 0.0d, 36.5d);
    }

    public static ProductSummaryResponse from(
            Product product,
            String thumbnailUrl,
            String categoryLabel,
            String tradeStatus,
            String tradeStatusLabel,
            long sellerReviewCount,
            double sellerAverageRating,
            double sellerMannerTemperature
    ) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getTitle(),
                product.getCategoryCode(),
                categoryLabel,
                product.getPrice(),
                product.isSold(),
                tradeStatus,
                tradeStatusLabel,
                thumbnailUrl,
                product.getViewCount(),
                product.getFavoriteCount(),
                product.getSeller().getId(),
                product.getSeller().getNickname(),
                sellerReviewCount,
                sellerAverageRating,
                sellerMannerTemperature,
                product.getCreatedAt()
        );
    }

    private static String defaultTradeStatus(Product product) {
        return product.isSold() ? "COMPLETED" : "ON_SALE";
    }

    private static String defaultTradeStatusLabel(Product product) {
        return product.isSold() ? "거래완료" : "판매중";
    }
}
