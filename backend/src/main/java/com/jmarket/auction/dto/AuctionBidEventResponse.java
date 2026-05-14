package com.jmarket.auction.dto;

public record AuctionBidEventResponse(
        String type,
        AuctionResponse auction,
        BidResponse bid
) {
    public static AuctionBidEventResponse bidPlaced(AuctionResponse auction, BidResponse bid) {
        return new AuctionBidEventResponse("BID_PLACED", auction, bid);
    }
}
