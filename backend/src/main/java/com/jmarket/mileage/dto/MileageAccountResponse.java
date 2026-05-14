package com.jmarket.mileage.dto;

import com.jmarket.mileage.domain.MileageAccount;

public record MileageAccountResponse(
        Long userId,
        Long balance,
        Long reservedBalance,
        Long withdrawPendingBalance,
        Long availableBalance
) {
    public static MileageAccountResponse from(MileageAccount account) {
        return new MileageAccountResponse(
                account.getUser().getId(),
                account.getBalance(),
                account.getReservedBalance(),
                account.getWithdrawPendingBalance(),
                account.getAvailableBalance()
        );
    }
}
