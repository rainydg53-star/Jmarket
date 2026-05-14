package com.jmarket.admin.dto;

import com.jmarket.auth.domain.User;
import com.jmarket.mileage.domain.MileageAccount;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String phoneNumber,
        String role,
        boolean banned,
        LocalDateTime bannedUntil,
        String banReason,
        LocalDateTime createdAt,
        Long mileageBalance,
        Long reservedMileage,
        Long availableMileage
) {
    public static AdminUserResponse from(User user) {
        return from(user, null);
    }

    public static AdminUserResponse from(User user, MileageAccount mileageAccount) {
        Long balance = mileageAccount == null ? 0L : mileageAccount.getBalance();
        Long reservedBalance = mileageAccount == null ? 0L : mileageAccount.getReservedBalance();
        Long availableBalance = mileageAccount == null ? 0L : mileageAccount.getAvailableBalance();
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isBanned(),
                user.getBannedUntil(),
                user.getBanReason(),
                user.getCreatedAt(),
                balance,
                reservedBalance,
                availableBalance
        );
    }
}
