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
@Table(name = "mileage_ledger")
public class MileageLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MileageLedgerType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false)
    private Long reservedAfter;

    @Column(nullable = false, length = 40)
    private String refType;

    @Column(nullable = false)
    private Long refId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected MileageLedger() {
    }

    public MileageLedger(
            User user,
            MileageLedgerType type,
            Long amount,
            Long balanceAfter,
            Long reservedAfter,
            String refType,
            Long refId
    ) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reservedAfter = reservedAfter;
        this.refType = refType;
        this.refId = refId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public MileageLedgerType getType() {
        return type;
    }

    public Long getAmount() {
        return amount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public Long getReservedAfter() {
        return reservedAfter;
    }

    public String getRefType() {
        return refType;
    }

    public Long getRefId() {
        return refId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
