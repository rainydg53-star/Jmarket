package com.jmarket.auction.dto;

public record AuctionBidSnapshot(
        Long currentHighestBid,
        Long currentHighestBidderId,
        String currentHighestBidderNickname,
        Long totalBidCount
) {
}
