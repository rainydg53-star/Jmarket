package com.jmarket.admin.domain;

import com.jmarket.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User adminUser;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 80)
    private String targetType;

    @Column
    private Long targetId;

    @Column(length = 1000)
    private String memo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AdminAuditLog() {
    }

    public AdminAuditLog(User adminUser, String action, String targetType, Long targetId, String memo) {
        this.adminUser = adminUser;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.memo = memo;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getAdminUser() {
        return adminUser;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getMemo() {
        return memo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
