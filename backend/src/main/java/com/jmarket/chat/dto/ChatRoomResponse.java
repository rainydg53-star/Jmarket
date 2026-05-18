package com.jmarket.chat.dto;

import com.jmarket.chat.domain.ChatRoom;
import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long id,
        String roomType,
        Long tradeId,
        String tradeStatus,
        String tradeProductTitle,
        Long auctionId,
        String auctionProductTitle,
        Long participantAId,
        String participantANickname,
        Long participantALastReadMessageId,
        Long participantBId,
        String participantBNickname,
        Long participantBLastReadMessageId,
        Long myLastReadMessageId,
        Long unreadCount,
        LocalDateTime createdAt,
        LocalDateTime lastMessageAt,
        Long lastMessageSenderId,
        String lastMessageSenderNickname,
        String lastMessageContent
) {
    public static ChatRoomResponse from(
            ChatRoom room,
            Long myUserId,
            Long unreadCount,
            Long lastMessageSenderId,
            String lastMessageSenderNickname,
            String lastMessageContent
    ) {
        return new ChatRoomResponse(
                room.getId(),
                room.getRoomType().name(),
                room.getTrade() != null ? room.getTrade().getId() : null,
                room.getTrade() != null ? room.getTrade().getStatus().name() : null,
                room.getTrade() != null ? room.getTrade().getProduct().getTitle() : null,
                room.getAuction() != null ? room.getAuction().getId() : null,
                room.getAuction() != null ? room.getAuction().getProduct().getTitle() : null,
                room.getParticipantA().getId(),
                room.getParticipantA().getNickname(),
                room.getParticipantALastReadMessageId(),
                room.getParticipantB().getId(),
                room.getParticipantB().getNickname(),
                room.getParticipantBLastReadMessageId(),
                room.getLastReadMessageIdFor(myUserId),
                unreadCount,
                room.getCreatedAt(),
                room.getLastMessageAt(),
                lastMessageSenderId,
                lastMessageSenderNickname,
                lastMessageContent
        );
    }
}
