package com.jmarket.trade.dto;

import com.jmarket.trade.domain.Trade;
import java.time.LocalDateTime;

public record TradeResponse(
        Long id,
        Long productId,
        String productTitle,
        Long buyerId,
        String buyerNickname,
        Long sellerId,
        String sellerNickname,
        Long offeredPrice,
        String paymentMethod,
        Long reservedMileageAmount,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime acceptedAt,
        boolean buyerConfirmedReceived,
        LocalDateTime buyerConfirmedAt,
        boolean sellerConfirmedHanded,
        LocalDateTime sellerConfirmedAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        boolean reviewedByMe,
        Long myReviewId,
        Long reviewTargetUserId,
        String reviewTargetUserNickname
) {
    public static TradeResponse from(Trade trade) {
        return from(trade, null, false, null);
    }

    public static TradeResponse from(Trade trade, Long viewerUserId, boolean reviewedByMe, Long myReviewId) {
        Long reviewTargetUserId = null;
        String reviewTargetUserNickname = null;
        if (viewerUserId != null) {
            boolean viewerIsBuyer = trade.getBuyer().getId().equals(viewerUserId);
            reviewTargetUserId = viewerIsBuyer ? trade.getSeller().getId() : trade.getBuyer().getId();
            reviewTargetUserNickname = viewerIsBuyer ? trade.getSeller().getNickname() : trade.getBuyer().getNickname();
        }
        return new TradeResponse(
                trade.getId(),
                trade.getProduct().getId(),
                trade.getProduct().getTitle(),
                trade.getBuyer().getId(),
                trade.getBuyer().getNickname(),
                trade.getSeller().getId(),
                trade.getSeller().getNickname(),
                trade.getOfferedPrice(),
                trade.getPaymentMethod().name(),
                trade.getReservedMileageAmount(),
                trade.getStatus().name(),
                trade.getRequestedAt(),
                trade.getAcceptedAt(),
                trade.isBuyerConfirmedReceived(),
                trade.getBuyerConfirmedAt(),
                trade.isSellerConfirmedHanded(),
                trade.getSellerConfirmedAt(),
                trade.getCompletedAt(),
                trade.getCanceledAt(),
                reviewedByMe,
                myReviewId,
                reviewTargetUserId,
                reviewTargetUserNickname
        );
    }
}
