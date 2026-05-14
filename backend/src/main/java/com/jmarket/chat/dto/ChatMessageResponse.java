package com.jmarket.chat.dto;

import com.jmarket.chat.domain.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        LocalDateTime sentAt,
        LocalDateTime readAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getSender().getId(),
                message.getSender().getNickname(),
                message.getContent(),
                message.getSentAt(),
                message.getReadAt()
        );
    }
}
