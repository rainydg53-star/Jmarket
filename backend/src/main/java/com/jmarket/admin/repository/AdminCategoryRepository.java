package com.jmarket.admin.repository;

import com.jmarket.admin.domain.AdminCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminCategoryRepository extends JpaRepository<AdminCategory, Long> {
    boolean existsByCode(String code);

    Optional<AdminCategory> findByCode(String code);

    List<AdminCategory> findAllByOrderByDisplayOrderAscIdAsc();

    List<AdminCategory> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
}
