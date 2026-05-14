package com.jmarket.mileage.domain;

import com.jmarket.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "mileage_accounts")
public class MileageAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Long balance;

    @Column(nullable = false)
    private Long reservedBalance;

    @Column(nullable = false)
    private Long withdrawPendingBalance;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected MileageAccount() {
    }

    public MileageAccount(User user) {
        this.user = user;
        this.balance = 0L;
        this.reservedBalance = 0L;
        this.withdrawPendingBalance = 0L;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void addBalance(Long amount) {
        this.balance = Math.addExact(this.balance, amount);
    }

    public void reserve(Long amount) {
        this.reservedBalance = Math.addExact(this.reservedBalance, amount);
    }

    public void releaseReservation(Long amount) {
        this.reservedBalance = Math.subtractExact(this.reservedBalance, amount);
    }

    public void debitReserved(Long amount) {
        this.balance = Math.subtractExact(this.balance, amount);
        this.reservedBalance = Math.subtractExact(this.reservedBalance, amount);
    }

    public void debitBalance(Long amount) {
        this.balance = Math.subtractExact(this.balance, amount);
    }

    public void requestWithdrawal(Long amount) {
        this.withdrawPendingBalance = Math.addExact(this.withdrawPendingBalance, amount);
    }

    public void completeWithdrawal(Long amount) {
        this.balance = Math.subtractExact(this.balance, amount);
        this.withdrawPendingBalance = Math.subtractExact(this.withdrawPendingBalance, amount);
    }

    public void releaseWithdrawal(Long amount) {
        this.withdrawPendingBalance = Math.subtractExact(this.withdrawPendingBalance, amount);
    }

    public Long getAvailableBalance() {
        return balance - reservedBalance - withdrawPendingBalance;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Long getBalance() {
        return balance;
    }

    public Long getReservedBalance() {
        return reservedBalance;
    }

    public Long getWithdrawPendingBalance() {
        return withdrawPendingBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
