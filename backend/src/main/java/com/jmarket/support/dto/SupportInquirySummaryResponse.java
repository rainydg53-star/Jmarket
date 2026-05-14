package com.jmarket.support.dto;

import com.jmarket.support.domain.SupportInquiry;
import com.jmarket.support.domain.SupportInquiryStatus;
import com.jmarket.support.domain.SupportMajorCategory;
import com.jmarket.support.domain.SupportMinorCategory;
import java.time.LocalDateTime;

public record SupportInquirySummaryResponse(
        Long id,
        SupportMajorCategory majorCategory,
        SupportMinorCategory minorCategory,
        String title,
        SupportInquiryStatus status,
        LocalDateTime createdAt
) {
    public static SupportInquirySummaryResponse from(SupportInquiry inquiry) {
        return new SupportInquirySummaryResponse(
                inquiry.getId(),
                inquiry.getMajorCategory(),
                inquiry.getMinorCategory(),
                inquiry.getTitle(),
                inquiry.getStatus(),
                inquiry.getCreatedAt()
        );
    }
}
