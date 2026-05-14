package com.jmarket.auth.dto;

import com.jmarket.product.dto.ProductSummaryResponse;
import com.jmarket.review.dto.ReviewResponse;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String nickname,
        long reviewCount,
        double averageRating,
        double mannerTemperature,
        long sellingProductCount,
        List<ProductSummaryResponse> sellingProducts,
        List<ReviewResponse> reviews
) {
}
