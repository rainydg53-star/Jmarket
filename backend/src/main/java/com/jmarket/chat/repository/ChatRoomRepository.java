package com.jmarket.chat.repository;

import com.jmarket.chat.domain.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
            select r from ChatRoom r
            where r.trade.id = :tradeId
              and ((r.participantA.id = :userId1 and r.participantB.id = :userId2)
                or (r.participantA.id = :userId2 and r.participantB.id = :userId1))
            """)
    Optional<ChatRoom> findByTradeAndParticipants(Long tradeId, Long userId1, Long userId2);

    @Query("""
            select r from ChatRoom r
            where r.auction.id = :auctionId
              and ((r.participantA.id = :userId1 and r.participantB.id = :userId2)
                or (r.participantA.id = :userId2 and r.participantB.id = :userId1))
            """)
    Optional<ChatRoom> findByAuctionAndParticipants(Long auctionId, Long userId1, Long userId2);

    @Query("""
            select r from ChatRoom r
            where r.participantA.id = :userId or r.participantB.id = :userId
            order by case when r.lastMessageAt is null then 1 else 0 end, r.lastMessageAt desc, r.createdAt desc
            """)
    List<ChatRoom> findMyRooms(Long userId);
}
