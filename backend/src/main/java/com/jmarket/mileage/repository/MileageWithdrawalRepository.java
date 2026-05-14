package com.jmarket.mileage.repository;

import com.jmarket.mileage.domain.MileageWithdrawal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MileageWithdrawalRepository extends JpaRepository<MileageWithdrawal, Long> {
    List<MileageWithdrawal> findAllByUserIdOrderByRequestedAtDesc(Long userId);

    List<MileageWithdrawal> findAllByOrderByRequestedAtDesc();
}
