package com.jmarket.product.dto;

import com.jmarket.product.domain.Product;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String title,
        String description,
        String category,
        String categoryLabel,
        Long price,
        boolean sold,
        String tradeStatus,
        String tradeStatusLabel,
        long viewCount,
        long favoriteCount,
        boolean favorited,
        List<ProductImageResponse> images,
        Long sellerId,
        String sellerNickname,
        long sellerReviewCount,
        double sellerAverageRating,
        double sellerMannerTemperature,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductDetailResponse from(Product product) {
        return from(product, List.of(), false, product.getCategoryCode());
    }

    public static ProductDetailResponse from(
            Product product,
            List<ProductImageResponse> images,
            boolean favorited
    ) {
        return from(product, images, favorited, product.getCategoryCode());
    }

    public static ProductDetailResponse from(
            Product product,
            List<ProductImageResponse> images,
            boolean favorited,
            String categoryLabel
    ) {
        return from(product, images, favorited, categoryLabel, defaultTradeStatus(product), defaultTradeStatusLabel(product), 0L, 0.0d, 36.5d);
    }

    public static ProductDetailResponse from(
            Product product,
            List<ProductImageResponse> images,
            boolean favorited,
            String categoryLabel,
            String tradeStatus,
            String tradeStatusLabel,
            long sellerReviewCount,
            double sellerAverageRating,
            double sellerMannerTemperature
    ) {
        return new ProductDetailResponse(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getCategoryCode(),
                categoryLabel,
                product.getPrice(),
                product.isSold(),
                tradeStatus,
                tradeStatusLabel,
                product.getViewCount(),
                product.getFavoriteCount(),
                favorited,
                images,
                product.getSeller().getId(),
                product.getSeller().getNickname(),
                sellerReviewCount,
                sellerAverageRating,
                sellerMannerTemperature,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private static String defaultTradeStatus(Product product) {
        return product.isSold() ? "COMPLETED" : "ON_SALE";
    }

    private static String defaultTradeStatusLabel(Product product) {
        return product.isSold() ? "거래완료" : "판매중";
    }
}
