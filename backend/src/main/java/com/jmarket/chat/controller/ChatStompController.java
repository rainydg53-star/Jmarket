package com.jmarket.chat.controller;

import com.jmarket.chat.dto.ChatMessageResponse;
import com.jmarket.chat.dto.ChatSendRequest;
import com.jmarket.chat.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatStompController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload ChatSendRequest request, Principal principal) {
        if (principal == null) {
            throw new org.springframework.messaging.MessagingException("Unauthorized stomp access");
        }
        ChatMessageResponse response = chatService.sendMessage(request.roomId(), request.content(), principal.getName());
        messagingTemplate.convertAndSend("/topic/chat.rooms." + request.roomId(), response);
    }
}
