package com.jmarket.chat.repository;

import com.jmarket.chat.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByRoomIdOrderBySentAtAsc(Long roomId);

    Optional<ChatMessage> findTopByRoomIdOrderByIdDesc(Long roomId);

    long countByRoomIdAndIdGreaterThan(Long roomId, Long messageId);

    long countByRoomId(Long roomId);
}
