package com.jmarket.review.dto;

public record ReviewSummaryResponse(
        Long userId,
        long reviewCount,
        double averageRating,
        double mannerTemperature
) {
}
