package com.jmarket.chat.controller;

import com.jmarket.chat.dto.ChatMessageResponse;
import com.jmarket.chat.dto.ChatRoomResponse;
import com.jmarket.chat.dto.CreateAuctionChatRoomRequest;
import com.jmarket.chat.dto.CreateTradeChatRoomRequest;
import com.jmarket.chat.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatService chatService;

    public ChatRoomController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/trade")
    public ChatRoomResponse createTradeRoom(
            @Valid @RequestBody CreateTradeChatRoomRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return chatService.createTradeRoom(request.tradeId(), email);
    }

    @PostMapping("/auction")
    public ChatRoomResponse createAuctionRoom(
            @Valid @RequestBody CreateAuctionChatRoomRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return chatService.createAuctionRoom(request.auctionId(), request.counterpartyUserId(), email);
    }

    @GetMapping("/me")
    public List<ChatRoomResponse> getMyRooms(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return chatService.getMyRooms(email);
    }

    @GetMapping("/{roomId}")
    public ChatRoomResponse getRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return chatService.getRoom(roomId, email);
    }

    @GetMapping("/{roomId}/messages")
    public List<ChatMessageResponse> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return chatService.getMessages(roomId, email);
    }

    @PatchMapping("/{roomId}/read")
    public ChatRoomResponse markRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return chatService.markRoomRead(roomId, email);
    }
}
