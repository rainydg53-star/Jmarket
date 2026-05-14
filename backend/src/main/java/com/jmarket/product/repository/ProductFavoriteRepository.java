package com.jmarket.product.repository;

import com.jmarket.product.domain.ProductFavorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, Long> {
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    Optional<ProductFavorite> findByProductIdAndUserId(Long productId, Long userId);

    List<ProductFavorite> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteAllByProductId(Long productId);
}
