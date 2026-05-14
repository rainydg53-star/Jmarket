package com.jmarket.trade.dto;

import jakarta.validation.constraints.NotNull;

public record TradeCreateRequest(
        @NotNull Long productId
) {
}
