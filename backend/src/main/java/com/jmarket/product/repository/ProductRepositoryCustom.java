package com.jmarket.product.repository;

import com.jmarket.product.domain.Product;
import com.jmarket.product.domain.ProductListingType;
import java.util.List;

public interface ProductRepositoryCustom {
    List<Product> searchProducts(ProductListingType listingType, String keyword, String category, String sort);
}
