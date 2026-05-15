package com.jmarket.auth.repository;

import com.jmarket.auth.domain.User;
import com.jmarket.auth.domain.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findAllByName(String name);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findAllByRole(UserRole role);

    List<User> findAllByRoleIn(List<UserRole> roles);
}
