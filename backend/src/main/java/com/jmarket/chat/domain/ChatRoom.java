package com.jmarket.chat.domain;

import com.jmarket.auction.domain.Auction;
import com.jmarket.auth.domain.User;
import com.jmarket.trade.domain.Trade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id")
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_a_id", nullable = false)
    private User participantA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_b_id", nullable = false)
    private User participantB;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastMessageAt;

    @Column
    private Long participantALastReadMessageId;

    @Column
    private Long participantBLastReadMessageId;

    protected ChatRoom() {
    }

    private ChatRoom(
            ChatRoomType roomType,
            Trade trade,
            Auction auction,
            User participantA,
            User participantB
    ) {
        this.roomType = roomType;
        this.trade = trade;
        this.auction = auction;
        this.participantA = participantA;
        this.participantB = participantB;
    }

    public static ChatRoom forTrade(Trade trade, User participantA, User participantB) {
        return new ChatRoom(ChatRoomType.PRODUCT_TRADE, trade, null, participantA, participantB);
    }

    public static ChatRoom forAuction(Auction auction, User participantA, User participantB) {
        return new ChatRoom(ChatRoomType.AUCTION_BID, null, auction, participantA, participantB);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateLastMessageAt(LocalDateTime messageTime) {
        this.lastMessageAt = messageTime;
    }

    public boolean isParticipant(Long userId) {
        return participantA.getId().equals(userId) || participantB.getId().equals(userId);
    }

    public Long getLastReadMessageIdFor(Long userId) {
        if (participantA.getId().equals(userId)) {
            return participantALastReadMessageId;
        }
        if (participantB.getId().equals(userId)) {
            return participantBLastReadMessageId;
        }
        return null;
    }

    public void markRead(Long userId, Long messageId) {
        if (participantA.getId().equals(userId)) {
            this.participantALastReadMessageId = messageId;
            return;
        }
        if (participantB.getId().equals(userId)) {
            this.participantBLastReadMessageId = messageId;
        }
    }

    public Long getId() {
        return id;
    }

    public ChatRoomType getRoomType() {
        return roomType;
    }

    public Trade getTrade() {
        return trade;
    }

    public Auction getAuction() {
        return auction;
    }

    public User getParticipantA() {
        return participantA;
    }

    public User getParticipantB() {
        return participantB;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public Long getParticipantALastReadMessageId() {
        return participantALastReadMessageId;
    }

    public Long getParticipantBLastReadMessageId() {
        return participantBLastReadMessageId;
    }
}
