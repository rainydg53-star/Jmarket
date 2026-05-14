package com.jmarket.auction.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import com.jmarket.product.dto.ProductImageRequest;

public record AuctionCreateRequest(
        Long productId,
        @Size(max = 100) String title,
        @Size(max = 20000) String description,
        @Size(max = 40) String category,
        @NotNull @Min(0) Long startPrice,
        @Min(1) Long instantBuyPrice,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @Size(max = 10) List<@Valid ProductImageRequest> images
) {
}
