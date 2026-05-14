package com.jmarket.admin.service;

import com.jmarket.admin.domain.UserRestriction;
import com.jmarket.admin.domain.UserRestrictionType;
import com.jmarket.admin.repository.UserRestrictionRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRestrictionService {

    private final UserRestrictionRepository restrictionRepository;

    public UserRestrictionService(UserRestrictionRepository restrictionRepository) {
        this.restrictionRepository = restrictionRepository;
    }

    @Transactional(readOnly = true)
    public void validateAllowed(Long userId, UserRestrictionType type) {
        restrictionRepository.findAllByUserIdAndTypeAndActiveTrueOrderByCreatedAtDesc(userId, type).stream()
                .filter(UserRestriction::isCurrentlyActive)
                .findFirst()
                .ifPresent(this::throwRestrictedException);
    }

    private void throwRestrictedException(UserRestriction restriction) {
        StringBuilder message = new StringBuilder("현재 이용이 제한된 기능입니다.");
        message.append(" 기능: ").append(restriction.getType().label());
        if (restriction.getReason() != null && !restriction.getReason().isBlank()) {
            message.append(" 사유: ").append(restriction.getReason());
        }
        if (restriction.getRestrictedUntil() != null) {
            message.append(" 해제 예정: ").append(restriction.getRestrictedUntil());
        }
        throw new JmarketException(ErrorCode.USER_RESTRICTED, message.toString());
    }
}
