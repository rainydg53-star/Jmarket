package com.jmarket.product.repository;

import com.jmarket.product.domain.ProductQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {
    List<ProductQuestion> findAllByProductIdOrderByCreatedAtDesc(Long productId);

    void deleteAllByProductId(Long productId);
}
