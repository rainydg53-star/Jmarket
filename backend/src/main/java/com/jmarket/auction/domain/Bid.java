package com.jmarket.auction.domain;

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
import java.time.Instant;

@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Instant bidAt;

    protected Bid() {
    }

    public Bid(Auction auction, User bidder, Long amount) {
        this.auction = auction;
        this.bidder = bidder;
        this.amount = amount;
    }

    @PrePersist
    protected void onCreate() {
        this.bidAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Auction getAuction() {
        return auction;
    }

    public User getBidder() {
        return bidder;
    }

    public Long getAmount() {
        return amount;
    }

    public Instant getBidAt() {
        return bidAt;
    }
}
