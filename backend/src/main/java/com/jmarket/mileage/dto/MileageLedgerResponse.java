package com.jmarket.mileage.dto;

import com.jmarket.mileage.domain.MileageLedger;
import java.time.LocalDateTime;

public record MileageLedgerResponse(
        Long id,
        String type,
        Long amount,
        Long balanceAfter,
        Long reservedAfter,
        String refType,
        Long refId,
        LocalDateTime createdAt
) {
    public static MileageLedgerResponse from(MileageLedger ledger) {
        return new MileageLedgerResponse(
                ledger.getId(),
                ledger.getType().name(),
                ledger.getAmount(),
                ledger.getBalanceAfter(),
                ledger.getReservedAfter(),
                ledger.getRefType(),
                ledger.getRefId(),
                ledger.getCreatedAt()
        );
    }
}
