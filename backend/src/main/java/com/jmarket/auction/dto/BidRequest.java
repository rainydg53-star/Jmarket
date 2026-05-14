package com.jmarket.auction.dto;

import jakarta.validation.constraints.Min;

public record BidRequest(
        @Min(0) Long amount
) {
}
