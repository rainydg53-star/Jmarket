package com.jmarket.product.repository;

import com.jmarket.product.domain.Product;
import com.jmarket.product.domain.ProductListingType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    List<Product> findAllByListingTypeOrderByCreatedAtDesc(ProductListingType listingType);

    List<Product> findAllBySellerIdAndListingTypeOrderByCreatedAtDesc(Long sellerId, ProductListingType listingType);

    Optional<Product> findByIdAndListingType(Long id, ProductListingType listingType);
}
