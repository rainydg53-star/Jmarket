package com.jmarket.chat.dto;

import jakarta.validation.constraints.NotNull;

public record CreateTradeChatRoomRequest(
        @NotNull Long tradeId
) {
}
