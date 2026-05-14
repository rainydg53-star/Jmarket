package com.jmarket.auction.repository;

import com.jmarket.auction.domain.Auction;
import java.time.Instant;
import java.util.List;

public interface AuctionRepositoryCustom {
    List<Auction> searchVisibleAuctions(String keyword, String category, String sort, Instant closedCutoff);
}
