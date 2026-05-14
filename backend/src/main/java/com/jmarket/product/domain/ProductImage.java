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

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private boolean thumbnail;

    @Column(nullable = false)
    private int sortOrder;

    protected ProductImage() {
    }

    public ProductImage(Product product, String imageUrl, boolean thumbnail, int sortOrder) {
        this.product = product;
        this.imageUrl = imageUrl;
        this.thumbnail = thumbnail;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isThumbnail() {
        return thumbnail;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
