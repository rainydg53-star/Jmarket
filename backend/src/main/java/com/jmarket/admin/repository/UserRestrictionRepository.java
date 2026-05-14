package com.jmarket.admin.repository;

import com.jmarket.admin.domain.UserRestriction;
import com.jmarket.admin.domain.UserRestrictionType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRestrictionRepository extends JpaRepository<UserRestriction, Long> {
    List<UserRestriction> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserRestriction> findAllByOrderByCreatedAtDesc();

    List<UserRestriction> findAllByUserIdAndTypeAndActiveTrueOrderByCreatedAtDesc(Long userId, UserRestrictionType type);

    boolean existsByUserIdAndTypeAndActiveTrueAndRestrictedUntilIsNull(Long userId, UserRestrictionType type);

    boolean existsByUserIdAndTypeAndActiveTrueAndRestrictedUntilAfter(
            Long userId,
            UserRestrictionType type,
            LocalDateTime now
    );
}
