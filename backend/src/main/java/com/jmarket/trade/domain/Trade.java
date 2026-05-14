package com.jmarket.trade.domain;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private Long offeredPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradePaymentMethod paymentMethod;

    @Column(nullable = false)
    private Long reservedMileageAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column
    private LocalDateTime acceptedAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean buyerConfirmedReceived;

    @Column
    private LocalDateTime buyerConfirmedAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean sellerConfirmedHanded;

    @Column
    private LocalDateTime sellerConfirmedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime canceledAt;

    protected Trade() {
    }

    public Trade(
            Product product,
            User buyer,
            User seller,
            Long offeredPrice,
            TradePaymentMethod paymentMethod,
            Long reservedMileageAmount
    ) {
        this.product = product;
        this.buyer = buyer;
        this.seller = seller;
        this.offeredPrice = offeredPrice;
        this.paymentMethod = paymentMethod;
        this.reservedMileageAmount = reservedMileageAmount;
        this.status = TradeStatus.REQUESTED;
    }

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
    }

    public void accept() {
        this.status = TradeStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public boolean confirmBuyerReceived() {
        if (buyerConfirmedReceived) {
            return false;
        }
        this.buyerConfirmedReceived = true;
        this.buyerConfirmedAt = LocalDateTime.now();
        return true;
    }

    public boolean confirmSellerHanded() {
        if (sellerConfirmedHanded) {
            return false;
        }
        this.sellerConfirmedHanded = true;
        this.sellerConfirmedAt = LocalDateTime.now();
        return true;
    }

    public boolean isBothSidesConfirmed() {
        return buyerConfirmedReceived && sellerConfirmedHanded;
    }

    public void complete() {
        this.status = TradeStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TradeStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public User getBuyer() {
        return buyer;
    }

    public User getSeller() {
        return seller;
    }

    public Long getOfferedPrice() {
        return offeredPrice;
    }

    public TradePaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Long getReservedMileageAmount() {
        return reservedMileageAmount;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public boolean isBuyerConfirmedReceived() {
        return buyerConfirmedReceived;
    }

    public LocalDateTime getBuyerConfirmedAt() {
        return buyerConfirmedAt;
    }

    public boolean isSellerConfirmedHanded() {
        return sellerConfirmedHanded;
    }

    public LocalDateTime getSellerConfirmedAt() {
        return sellerConfirmedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }
}
