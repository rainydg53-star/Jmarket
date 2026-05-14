package com.jmarket.product.repository;

import com.jmarket.product.domain.ProductImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findAllByProductIdOrderBySortOrderAsc(Long productId);

    void deleteAllByProductId(Long productId);
}
