package com.jmarket.chat.service;

import com.jmarket.auction.domain.Auction;
import com.jmarket.auction.repository.AuctionRepository;
import com.jmarket.auction.repository.BidRepository;
import com.jmarket.auth.domain.User;
import com.jmarket.auth.domain.UserRole;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.chat.domain.ChatMessage;
import com.jmarket.chat.domain.ChatRoom;
import com.jmarket.chat.dto.ChatMessageResponse;
import com.jmarket.chat.dto.ChatRoomResponse;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.notification.service.NotificationEventService;
import com.jmarket.trade.domain.Trade;
import com.jmarket.trade.domain.TradeStatus;
import com.jmarket.trade.repository.TradeRepository;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final NotificationEventService notificationEventService;
    private final com.jmarket.chat.repository.ChatRoomRepository chatRoomRepository;
    private final com.jmarket.chat.repository.ChatMessageRepository chatMessageRepository;

    public ChatService(
            UserRepository userRepository,
            TradeRepository tradeRepository,
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            NotificationEventService notificationEventService,
            com.jmarket.chat.repository.ChatRoomRepository chatRoomRepository,
            com.jmarket.chat.repository.ChatMessageRepository chatMessageRepository
    ) {
        this.userRepository = userRepository;
        this.tradeRepository = tradeRepository;
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.notificationEventService = notificationEventService;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatRoomResponse createTradeRoom(Long tradeId, String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new JmarketException(ErrorCode.TRADE_NOT_FOUND));

        boolean isBuyer = trade.getBuyer().getId().equals(currentUser.getId());
        boolean isSeller = trade.getSeller().getId().equals(currentUser.getId());
        if (!isBuyer && !isSeller) {
            throw new JmarketException(ErrorCode.CHAT_FORBIDDEN_PARTICIPANT);
        }

        Long userA = trade.getBuyer().getId();
        Long userB = trade.getSeller().getId();
        ChatRoom room = chatRoomRepository.findByTradeAndParticipants(tradeId, userA, userB)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.forTrade(trade, trade.getBuyer(), trade.getSeller())));
        return toRoomResponse(room, currentUser.getId());
    }

    @Transactional
    public ChatRoomResponse createAuctionRoom(Long auctionId, Long counterpartyUserId, String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        User counterparty = userRepository.findById(counterpartyUserId)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        if (currentUser.getId().equals(counterparty.getId())) {
            throw new JmarketException(ErrorCode.CHAT_INVALID_REQUEST);
        }

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new JmarketException(ErrorCode.AUCTION_NOT_FOUND));
        User seller = auction.getSeller();

        boolean currentIsSeller = seller.getId().equals(currentUser.getId());
        boolean counterpartyIsSeller = seller.getId().equals(counterparty.getId());
        if (currentIsSeller == counterpartyIsSeller) {
            throw new JmarketException(ErrorCode.CHAT_FORBIDDEN_PARTICIPANT);
        }

        User bidder = currentIsSeller ? counterparty : currentUser;
        if (!bidRepository.existsByAuctionIdAndBidderId(auctionId, bidder.getId())) {
            throw new JmarketException(ErrorCode.CHAT_FORBIDDEN_PARTICIPANT);
        }

        Long userA = seller.getId();
        Long userB = bidder.getId();
        ChatRoom room = chatRoomRepository.findByAuctionAndParticipants(auctionId, userA, userB)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.forAuction(auction, seller, bidder)));
        return toRoomResponse(room, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        return chatRoomRepository.findMyRooms(currentUser.getId()).stream()
                .map(room -> toRoomResponse(room, currentUser.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        ChatRoom room = findRoomById(roomId);
        validateParticipantOrAdmin(room, currentUser);
        if (room.isParticipant(currentUser.getId())) {
            markReadToLatest(room, currentUser.getId());
        }
        return chatMessageRepository.findAllByRoomIdOrderBySentAtAsc(roomId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, String content, String currentUserEmail) {
        User sender = findUserByEmail(currentUserEmail);
        ChatRoom room = findRoomById(roomId);
        validateParticipant(room, sender);
        validateSendableRoom(room);

        ChatMessage chatMessage = new ChatMessage(room, sender, content.trim());
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        room.updateLastMessageAt(saved.getSentAt());
        room.markRead(sender.getId(), saved.getId());
        saved.markRead(saved.getSentAt());
        notificationEventService.notifyChatMessage(room, saved);
        return ChatMessageResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(Long roomId, String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        ChatRoom room = findRoomById(roomId);
        validateParticipantOrAdmin(room, currentUser);
        return toRoomResponse(room, currentUser.getId());
    }

    @Transactional
    public ChatRoomResponse markRoomRead(Long roomId, String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        ChatRoom room = findRoomById(roomId);
        validateParticipant(room, currentUser);
        markReadToLatest(room, currentUser.getId());
        return toRoomResponse(room, currentUser.getId());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }

    private ChatRoom findRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new JmarketException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private void validateParticipant(ChatRoom room, User user) {
        if (!room.isParticipant(user.getId())) {
            throw new JmarketException(ErrorCode.CHAT_FORBIDDEN_PARTICIPANT);
        }
    }

    private void validateParticipantOrAdmin(ChatRoom room, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        validateParticipant(room, user);
    }

    private void validateSendableRoom(ChatRoom room) {
        if (room.getTrade() != null && room.getTrade().getStatus() == TradeStatus.COMPLETED) {
            throw new JmarketException(ErrorCode.TRADE_INVALID_STATUS);
        }
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, Long myUserId) {
        Long lastReadMessageId = room.getLastReadMessageIdFor(myUserId);
        long unreadCount = (lastReadMessageId == null)
                ? chatMessageRepository.countByRoomId(room.getId())
                : chatMessageRepository.countByRoomIdAndIdGreaterThan(room.getId(), lastReadMessageId);
        ChatMessage latestMessage = chatMessageRepository.findTopByRoomIdOrderByIdDesc(room.getId()).orElse(null);
        return ChatRoomResponse.from(
                room,
                myUserId,
                unreadCount,
                latestMessage != null ? latestMessage.getSender().getId() : null,
                latestMessage != null ? latestMessage.getSender().getNickname() : null,
                latestMessage != null ? latestMessage.getContent() : null
        );
    }

    private void markReadToLatest(ChatRoom room, Long userId) {
        ChatMessage latest = chatMessageRepository.findTopByRoomIdOrderByIdDesc(room.getId()).orElse(null);
        if (latest == null) {
            return;
        }
        Long currentLastRead = room.getLastReadMessageIdFor(userId);
        if (currentLastRead != null && currentLastRead >= latest.getId()) {
            return;
        }
        room.markRead(userId, latest.getId());
        List<ChatMessage> messages = chatMessageRepository.findAllByRoomIdOrderBySentAtAsc(room.getId());
        LocalDateTime now = LocalDateTime.now();
        for (ChatMessage message : messages) {
            if (message.getReadAt() == null && !message.getSender().getId().equals(userId)) {
                message.markRead(now);
            }
        }
    }
}
