package com.jmarket.mileage.repository;

import com.jmarket.mileage.domain.MileageAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface MileageAccountRepository extends JpaRepository<MileageAccount, Long> {
    Optional<MileageAccount> findByUserId(Long userId);

    List<MileageAccount> findAllByUserIdIn(Collection<Long> userIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from MileageAccount a where a.user.id = :userId")
    Optional<MileageAccount> findByUserIdForUpdate(Long userId);
}
