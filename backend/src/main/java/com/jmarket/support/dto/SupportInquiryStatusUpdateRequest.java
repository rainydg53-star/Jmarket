package com.jmarket.support.dto;

import com.jmarket.support.domain.SupportInquiryStatus;
import jakarta.validation.constraints.NotNull;

public record SupportInquiryStatusUpdateRequest(
        @NotNull SupportInquiryStatus status
) {
}
