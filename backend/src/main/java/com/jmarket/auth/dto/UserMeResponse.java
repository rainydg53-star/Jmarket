package com.jmarket.auth.dto;

import com.jmarket.auth.domain.User;
import java.util.List;

public record UserMeResponse(
        Long id,
        String loginId,
        String name,
        String nickname,
        String phoneNumber,
        String role,
        boolean banned,
        List<String> activeRestrictions
) {
    public static UserMeResponse from(User user) {
        return from(user, List.of());
    }

    public static UserMeResponse from(User user, List<String> activeRestrictions) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isBanned(),
                activeRestrictions
        );
    }
}
