package com.jmarket.admin.domain;

import com.jmarket.auth.domain.User;
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
@Table(name = "user_restrictions")
public class UserRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserRestrictionType type;

    @Column(length = 500)
    private String reason;

    @Column
    private LocalDateTime restrictedUntil;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected UserRestriction() {
    }

    public UserRestriction(User user, UserRestrictionType type, String reason, LocalDateTime restrictedUntil) {
        this.user = user;
        this.type = type;
        this.reason = reason;
        this.restrictedUntil = restrictedUntil;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isCurrentlyActive() {
        return active && (restrictedUntil == null || restrictedUntil.isAfter(LocalDateTime.now()));
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public UserRestrictionType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getRestrictedUntil() {
        return restrictedUntil;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
