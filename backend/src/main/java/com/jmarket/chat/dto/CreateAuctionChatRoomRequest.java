package com.jmarket.chat.dto;

import jakarta.validation.constraints.NotNull;

public record CreateAuctionChatRoomRequest(
        @NotNull Long auctionId,
        @NotNull Long counterpartyUserId
) {
}
