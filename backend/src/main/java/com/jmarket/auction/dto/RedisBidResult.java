package com.jmarket.auction.dto;

public record RedisBidResult(
        boolean accepted,
        boolean topBidderAlready,
        Long previousHighestBid,
        Long previousHighestBidderId,
        String previousHighestBidderNickname,
        Long totalBidCount,
        Long effectiveBidAmount,
        Long minimumAllowed
) {
    public static RedisBidResult rejectedTooLow(Long currentHighestBid, Long minimumAllowed) {
        return new RedisBidResult(false, false, currentHighestBid, null, null, null, null, minimumAllowed);
    }

    public static RedisBidResult rejectedTopBidder(Long currentHighestBid, Long currentHighestBidderId) {
        return new RedisBidResult(false, true, currentHighestBid, currentHighestBidderId, null, null, null, null);
    }
}
