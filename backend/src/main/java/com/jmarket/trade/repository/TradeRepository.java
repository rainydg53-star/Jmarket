package com.jmarket.trade.repository;

import com.jmarket.trade.domain.Trade;
import com.jmarket.trade.domain.TradeStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    boolean existsByProductIdAndStatusIn(Long productId, Collection<TradeStatus> statuses);

    boolean existsByProductId(Long productId);

    Optional<Trade> findFirstByProductIdAndStatusInOrderByRequestedAtDesc(Long productId, Collection<TradeStatus> statuses);

    List<Trade> findAllByBuyerIdOrderByRequestedAtDesc(Long buyerId);

    List<Trade> findAllBySellerIdOrderByRequestedAtDesc(Long sellerId);

    long countByStatus(TradeStatus status);
}
