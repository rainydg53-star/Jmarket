package com.jmarket.support.dto;

import com.jmarket.support.domain.SupportInquiry;
import com.jmarket.support.domain.SupportInquiryStatus;
import com.jmarket.support.domain.SupportMajorCategory;
import com.jmarket.support.domain.SupportMinorCategory;
import java.time.LocalDateTime;

public record SupportInquiryDetailResponse(
        Long id,
        Long memberId,
        String memberEmail,
        String memberNickname,
        SupportMajorCategory majorCategory,
        SupportMinorCategory minorCategory,
        String title,
        String content,
        SupportInquiryStatus status,
        String answerContent,
        Long answeredById,
        String answeredByEmail,
        LocalDateTime answeredAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SupportInquiryDetailResponse from(SupportInquiry inquiry) {
        return new SupportInquiryDetailResponse(
                inquiry.getId(),
                inquiry.getMember().getId(),
                inquiry.getMember().getEmail(),
                inquiry.getMember().getNickname(),
                inquiry.getMajorCategory(),
                inquiry.getMinorCategory(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getAnswerContent(),
                inquiry.getAnsweredBy() != null ? inquiry.getAnsweredBy().getId() : null,
                inquiry.getAnsweredBy() != null ? inquiry.getAnsweredBy().getEmail() : null,
                inquiry.getAnsweredAt(),
                inquiry.getClosedAt(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }
}
