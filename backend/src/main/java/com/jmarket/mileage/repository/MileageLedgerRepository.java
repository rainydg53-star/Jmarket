package com.jmarket.mileage.repository;

import com.jmarket.mileage.domain.MileageLedger;
import com.jmarket.mileage.domain.MileageLedgerType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MileageLedgerRepository extends JpaRepository<MileageLedger, Long> {
    List<MileageLedger> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select coalesce(sum(l.amount), 0) from MileageLedger l where l.type = :type")
    Long sumAmountByType(MileageLedgerType type);
}
