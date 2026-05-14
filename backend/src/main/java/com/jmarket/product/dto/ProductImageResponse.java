package com.jmarket.product.dto;

import com.jmarket.product.domain.ProductImage;
import java.io.Serializable;

public record ProductImageResponse(
        Long id,
        String imageUrl,
        boolean thumbnail,
        int sortOrder
) implements Serializable {
    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.isThumbnail(),
                image.getSortOrder()
        );
    }
}
