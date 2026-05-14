package com.jmarket.admin.dto;

import com.jmarket.product.domain.Product;
import java.time.LocalDateTime;

public record AdminProductResponse(
        Long id,
        String title,
        String sellerNickname,
        String category,
        String categoryLabel,
        String listingType,
        Long price,
        boolean sold,
        LocalDateTime createdAt
) {
    public static AdminProductResponse from(Product product) {
        return from(product, product.getCategoryCode());
    }

    public static AdminProductResponse from(Product product, String categoryLabel) {
        return new AdminProductResponse(
                product.getId(),
                product.getTitle(),
                product.getSeller().getNickname(),
                product.getCategoryCode(),
                categoryLabel,
                product.getListingType().name(),
                product.getPrice(),
                product.isSold(),
                product.getCreatedAt()
        );
    }
}
