package com.jmarket.review.dto;

import com.jmarket.review.domain.UserReview;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long reviewerId,
        String reviewerNickname,
        Long targetUserId,
        String targetUserNickname,
        String sourceType,
        Long sourceId,
        String sourceTitle,
        int rating,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(UserReview review) {
        return from(review, null);
    }

    public static ReviewResponse from(UserReview review, String sourceTitle) {
        return new ReviewResponse(
                review.getId(),
                review.getReviewer().getId(),
                review.getReviewer().getNickname(),
                review.getTargetUser().getId(),
                review.getTargetUser().getNickname(),
                review.getSourceType().name(),
                review.getSourceId(),
                sourceTitle,
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
