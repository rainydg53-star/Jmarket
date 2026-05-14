package com.jmarket.payment.domain;

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
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 100)
    private String tid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 500)
    private String redirectUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private LocalDateTime failedAt;

    @Column
    private LocalDateTime canceledAt;

    @Column(length = 1000)
    private String failReason;

    protected Payment() {
    }

    public Payment(User user, String provider, String orderId, Long amount) {
        this.user = user;
        this.provider = provider;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void markReady(String tid, String redirectUrl) {
        this.tid = tid;
        this.redirectUrl = redirectUrl;
        this.status = PaymentStatus.PENDING;
    }

    public void markApproved() {
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.failReason = reason;
    }

    public void markCanceled(String reason) {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.failReason = reason;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getProvider() {
        return provider;
    }

    public String getOrderId() {
        return orderId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getTid() {
        return tid;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public String getFailReason() {
        return failReason;
    }
}
