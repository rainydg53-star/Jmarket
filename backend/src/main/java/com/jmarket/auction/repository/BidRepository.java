package com.jmarket.auction.repository;

import com.jmarket.auction.domain.Bid;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
    Optional<Bid> findTopByAuctionIdOrderByAmountDescBidAtAsc(Long auctionId);

    Optional<Bid> findTopByAuctionIdAndBidderIdOrderByAmountDescBidAtAsc(Long auctionId, Long bidderId);

    List<Bid> findAllByAuctionIdOrderByBidAtAsc(Long auctionId);

    long countByAuctionId(Long auctionId);

    boolean existsByAuctionIdAndBidderId(Long auctionId, Long bidderId);
}
