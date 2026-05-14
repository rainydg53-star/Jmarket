package com.jmarket.auction.dto;

import com.jmarket.auction.domain.Bid;
import java.time.Instant;

public record BidResponse(
        Long id,
        Long bidderId,
        String bidderNickname,
        Long amount,
        Instant bidAt
) {
    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getBidder().getId(),
                bid.getBidder().getNickname(),
                bid.getAmount(),
                bid.getBidAt()
        );
    }
}
