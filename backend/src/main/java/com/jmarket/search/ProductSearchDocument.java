package com.jmarket.search;

import com.jmarket.product.domain.Product;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "jmarket-products")
public class ProductSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String listingType;

    @Field(type = FieldType.Text)
    private String sellerNickname;

    @Field(type = FieldType.Long)
    private Long price;

    @Field(type = FieldType.Long)
    private Long viewCount;

    @Field(type = FieldType.Long)
    private Long favoriteCount;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    protected ProductSearchDocument() {
    }

    private ProductSearchDocument(Product product) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.description = product.getDescription();
        this.category = product.getCategoryCode();
        this.listingType = product.getListingType().name();
        this.sellerNickname = product.getSeller().getNickname();
        this.price = product.getPrice();
        this.viewCount = product.getViewCount();
        this.favoriteCount = product.getFavoriteCount();
        this.createdAt = product.getCreatedAt();
    }

    public static ProductSearchDocument from(Product product) {
        return new ProductSearchDocument(product);
    }

    public Long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getListingType() {
        return listingType;
    }

    public Long getPrice() {
        return price;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public Long getFavoriteCount() {
        return favoriteCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
