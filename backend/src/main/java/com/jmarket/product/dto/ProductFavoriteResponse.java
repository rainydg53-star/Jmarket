package com.jmarket.product.dto;

public record ProductFavoriteResponse(
        boolean favorited,
        long favoriteCount
) {
}
