package com.jmarket.mileage.dto;

import com.jmarket.mileage.domain.MileageWithdrawal;
import java.time.LocalDateTime;

public record MileageWithdrawalResponse(
        Long id,
        Long userId,
        String userEmail,
        String userNickname,
        Long amount,
        String bankName,
        String accountNumberMasked,
        String accountHolder,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime rejectedAt,
        String rejectReason
) {
    public static MileageWithdrawalResponse from(MileageWithdrawal withdrawal) {
        return new MileageWithdrawalResponse(
                withdrawal.getId(),
                withdrawal.getUser().getId(),
                withdrawal.getUser().getEmail(),
                withdrawal.getUser().getNickname(),
                withdrawal.getAmount(),
                withdrawal.getBankName(),
                withdrawal.getAccountNumberMasked(),
                withdrawal.getAccountHolder(),
                withdrawal.getStatus().name(),
                withdrawal.getRequestedAt(),
                withdrawal.getCompletedAt(),
                withdrawal.getRejectedAt(),
                withdrawal.getRejectReason()
        );
    }
}
