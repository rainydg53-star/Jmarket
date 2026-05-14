package com.jmarket.notification.repository;

import com.jmarket.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByRecipientUserIdOrderByIdDesc(Long recipientUserId);

    List<Notification> findAllByRecipientUserIdAndReadAtIsNullOrderByIdDesc(Long recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    @Modifying
    @Query("""
            update Notification n
            set n.readAt = :readAt
            where n.recipientUserId = :recipientUserId
              and n.readAt is null
            """)
    int markAllRead(@Param("recipientUserId") Long recipientUserId, @Param("readAt") Instant readAt);
}
