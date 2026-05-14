package com.jmarket.payment.repository;

import com.jmarket.payment.domain.Payment;
import com.jmarket.payment.domain.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = :status")
    Long sumAmountByStatus(PaymentStatus status);
}
