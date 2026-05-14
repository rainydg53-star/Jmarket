package com.jmarket.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatSendRequest(
        @NotNull Long roomId,
        @NotBlank @Size(max = 2000) String content
) {
}
