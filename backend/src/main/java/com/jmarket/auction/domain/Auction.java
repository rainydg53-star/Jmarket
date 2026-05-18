package com.jmarket.auction.domain;

import com.jmarket.auth.domain.User;
import com.jmarket.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
        name = "auctions",
        indexes = {
                @Index(name = "idx_auctions_status_end_at", columnList = "status, end_at"),
                @Index(name = "idx_auctions_status_closed_at", columnList = "status, closed_at"),
                @Index(name = "idx_auctions_winner_closed_at", columnList = "winner_user_id, status, closed_at"),
                @Index(name = "idx_auctions_product_status", columnList = "product_id, status")
        }
)
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private Long startPrice;

    // Legacy schema compatibility: keep internal column populated even though UI/API no longer exposes bid unit.
    @Column(nullable = false)
    private Long bidUnit;

    @Column
    private Long instantBuyPrice;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_user_id")
    private User winnerUser;

    @Column
    private Long winningBidAmount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant closedAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean hidden = false;

    protected Auction() {
    }

    public Auction(
            Product product,
            User seller,
            Long startPrice,
            Long instantBuyPrice,
            Instant startAt,
            Instant endAt
    ) {
        this.product = product;
        this.seller = seller;
        this.startPrice = startPrice;
        this.bidUnit = 1L;
        this.instantBuyPrice = instantBuyPrice;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = AuctionStatus.OPEN;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void close(User winnerUser, Long winningBidAmount) {
        this.status = AuctionStatus.CLOSED;
        this.winnerUser = winnerUser;
        this.winningBidAmount = winningBidAmount;
        this.closedAt = Instant.now();
    }

    public void cancel() {
        this.status = AuctionStatus.CLOSED;
        this.winnerUser = null;
        this.winningBidAmount = null;
        this.closedAt = Instant.now();
    }

    public void hide() {
        this.hidden = true;
    }

    public void show() {
        this.hidden = false;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public User getSeller() {
        return seller;
    }

    public Long getStartPrice() {
        return startPrice;
    }

    public Long getInstantBuyPrice() {
        return instantBuyPrice;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public User getWinnerUser() {
        return winnerUser;
    }

    public Long getWinningBidAmount() {
        return winningBidAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public boolean isHidden() {
        return hidden;
    }
}
