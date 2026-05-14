package com.jmarket.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "viewer_key"})
)
public class ProductView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "viewer_key", nullable = false, length = 255)
    private String viewerKey;

    @Column(nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();

    protected ProductView() {
    }

    public ProductView(Product product, String viewerKey) {
        this.product = product;
        this.viewerKey = viewerKey;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getViewerKey() {
        return viewerKey;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }
}
