package com.jmarket.notification.domain;

import com.jmarket.notification.dto.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(length = 255)
    private String link;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant readAt;

    protected Notification() {
    }

    public Notification(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            String link,
            Instant occurredAt
    ) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getLink() {
        return link;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
