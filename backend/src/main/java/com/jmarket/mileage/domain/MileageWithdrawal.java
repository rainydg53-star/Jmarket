package com.jmarket.mileage.domain;

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
@Table(name = "mileage_withdrawals")
public class MileageWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 40)
    private String bankName;

    @Column(nullable = false, length = 40)
    private String accountNumberMasked;

    @Column(nullable = false, length = 40)
    private String accountHolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MileageWithdrawalStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime rejectedAt;

    @Column(length = 500)
    private String rejectReason;

    protected MileageWithdrawal() {
    }

    public MileageWithdrawal(User user, Long amount, String bankName, String accountNumberMasked, String accountHolder) {
        this.user = user;
        this.amount = amount;
        this.bankName = bankName;
        this.accountNumberMasked = accountNumberMasked;
        this.accountHolder = accountHolder;
        this.status = MileageWithdrawalStatus.REQUESTED;
    }

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = MileageWithdrawalStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = MileageWithdrawalStatus.REJECTED;
        this.rejectReason = reason;
        this.rejectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Long getAmount() { return amount; }
    public String getBankName() { return bankName; }
    public String getAccountNumberMasked() { return accountNumberMasked; }
    public String getAccountHolder() { return accountHolder; }
    public MileageWithdrawalStatus getStatus() { return status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public String getRejectReason() { return rejectReason; }
}
