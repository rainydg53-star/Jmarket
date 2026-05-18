package com.jmarket.admin.dto;

import com.jmarket.auction.domain.Auction;
import java.time.Instant;

public record AdminAuctionResponse(
        Long id,
        String productTitle,
        String sellerNickname,
        Long startPrice,
        Long currentPrice,
        String status,
        boolean hidden,
        Instant endAt,
        Instant closedAt
) {
    public static AdminAuctionResponse from(Auction auction) {
        Long currentPrice = auction.getWinningBidAmount() != null ? auction.getWinningBidAmount() : auction.getStartPrice();
        return new AdminAuctionResponse(
                auction.getId(),
                auction.getProduct().getTitle(),
                auction.getSeller().getNickname(),
                auction.getStartPrice(),
                currentPrice,
                auction.getStatus().name(),
                auction.isHidden(),
                auction.getEndAt(),
                auction.getClosedAt()
        );
    }
}
