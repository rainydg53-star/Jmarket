package com.jmarket.auction.dto;

import com.jmarket.auction.domain.Auction;
import com.jmarket.product.dto.ProductImageResponse;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record AuctionResponse(
        Long id,
        Long productId,
        String productTitle,
        String productDescription,
        String category,
        String categoryLabel,
        String thumbnailUrl,
        List<ProductImageResponse> images,
        Long sellerId,
        String sellerNickname,
        Long startPrice,
        Long instantBuyPrice,
        Instant startAt,
        Instant endAt,
        String status,
        Long winnerUserId,
        String winnerNickname,
        Long winningBidAmount,
        Long currentHighestBid,
        Long currentHighestBidderId,
        String currentHighestBidderNickname,
        Long totalBidCount,
        Instant createdAt,
        Instant closedAt
) implements Serializable {
    public static AuctionResponse from(
            Auction auction,
            Long currentHighestBid,
            Long currentHighestBidderId,
            String currentHighestBidderNickname,
            Long totalBidCount,
            List<ProductImageResponse> images,
            String categoryLabel
    ) {
        String thumbnailUrl = images.stream()
                .filter(ProductImageResponse::thumbnail)
                .findFirst()
                .or(() -> images.stream().findFirst())
                .map(ProductImageResponse::imageUrl)
                .orElse(null);
        return new AuctionResponse(
                auction.getId(),
                auction.getProduct().getId(),
                auction.getProduct().getTitle(),
                auction.getProduct().getDescription(),
                auction.getProduct().getCategoryCode(),
                categoryLabel,
                thumbnailUrl,
                images,
                auction.getSeller().getId(),
                auction.getSeller().getNickname(),
                auction.getStartPrice(),
                auction.getInstantBuyPrice(),
                auction.getStartAt(),
                auction.getEndAt(),
                auction.getStatus().name(),
                auction.getWinnerUser() != null ? auction.getWinnerUser().getId() : null,
                auction.getWinnerUser() != null ? auction.getWinnerUser().getNickname() : null,
                auction.getWinningBidAmount(),
                currentHighestBid,
                currentHighestBidderId,
                currentHighestBidderNickname,
                totalBidCount,
                auction.getCreatedAt(),
                auction.getClosedAt()
        );
    }
}
