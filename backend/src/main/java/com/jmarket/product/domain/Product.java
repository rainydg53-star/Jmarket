package com.jmarket.product.domain;

import com.jmarket.auth.domain.User;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_listing_category_created", columnList = "listing_type, category, created_at"),
                @Index(name = "idx_products_listing_price", columnList = "listing_type, price"),
                @Index(name = "idx_products_listing_popular", columnList = "listing_type, favorite_count, view_count"),
                @Index(name = "idx_products_seller_listing_created", columnList = "seller_id, listing_type, created_at")
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 40, columnDefinition = "varchar(40) default 'ETC'")
    private String category = ProductCategory.ETC.name();

    @Column(nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'DIRECT'")
    private ProductListingType listingType = ProductListingType.DIRECT;

    @Column(nullable = false)
    private boolean sold = false;

    @Column(nullable = false)
    private long viewCount = 0L;

    @Column(nullable = false)
    private long favoriteCount = 0L;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Product() {
    }

    public Product(User seller, String title, String description, Long price) {
        this(seller, title, description, price, ProductListingType.DIRECT);
    }

    public Product(User seller, String title, String description, Long price, ProductListingType listingType) {
        this(seller, title, description, ProductCategory.ETC.name(), price, listingType);
    }

    public Product(
            User seller,
            String title,
            String description,
            String category,
            Long price,
            ProductListingType listingType
    ) {
        this.seller = seller;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.listingType = listingType;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String description, Long price) {
        update(title, description, this.category, price);
    }

    public void update(String title, String description, String category, Long price) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseFavoriteCount() {
        this.favoriteCount++;
    }

    public void decreaseFavoriteCount() {
        if (this.favoriteCount > 0) {
            this.favoriteCount--;
        }
    }

    public void markSold() {
        this.sold = true;
    }

    public Long getId() {
        return id;
    }

    public User getSeller() {
        return seller;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryCode() {
        return category;
    }

    public Long getPrice() {
        return price;
    }

    public ProductListingType getListingType() {
        return listingType;
    }

    public boolean isSold() {
        return sold;
    }

    public long getViewCount() {
        return viewCount;
    }

    public long getFavoriteCount() {
        return favoriteCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
