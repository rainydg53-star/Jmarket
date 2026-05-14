package com.jmarket.product.repository;

import com.jmarket.product.domain.ProductView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {
    boolean existsByProductIdAndViewerKey(Long productId, String viewerKey);

    List<ProductView> findTop20ByViewerKeyOrderByViewedAtDesc(String viewerKey);

    void deleteAllByProductId(Long productId);
}
