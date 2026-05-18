package com.jmarket.auction.repository;

import com.jmarket.auction.domain.Auction;
import com.jmarket.auction.domain.AuctionStatus;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AuctionRepository extends JpaRepository<Auction, Long>, AuctionRepositoryCustom {
    boolean existsByProductIdAndStatus(Long productId, AuctionStatus status);

    boolean existsByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :auctionId")
    Optional<Auction> findByIdForUpdate(Long auctionId);

    List<Auction> findAllByStatusOrderByEndAtAsc(AuctionStatus status);

    long countByStatus(AuctionStatus status);

    List<Auction> findAllByStatusAndWinnerUserIdAndHiddenFalseOrderByClosedAtDesc(AuctionStatus status, Long winnerUserId);

    List<Auction> findAllByStatusAndEndAtLessThanEqualAndHiddenFalseOrderByEndAtAsc(AuctionStatus status, java.time.Instant endAt);

    List<Auction> findAllByStatusAndClosedAtGreaterThanEqualAndHiddenFalseOrderByClosedAtDesc(
            AuctionStatus status,
            java.time.Instant closedAt
    );
}
