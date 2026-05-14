package com.jmarket.review.service;

import com.jmarket.auction.domain.Auction;
import com.jmarket.auction.domain.AuctionStatus;
import com.jmarket.auction.repository.AuctionRepository;
import com.jmarket.auth.domain.User;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.notification.service.NotificationEventService;
import com.jmarket.review.domain.ReviewSourceType;
import com.jmarket.review.domain.UserReview;
import com.jmarket.review.dto.ReviewCreateRequest;
import com.jmarket.review.dto.ReviewResponse;
import com.jmarket.review.dto.ReviewSummaryResponse;
import com.jmarket.review.repository.UserReviewRepository;
import com.jmarket.trade.domain.Trade;
import com.jmarket.trade.domain.TradeStatus;
import com.jmarket.trade.repository.TradeRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final UserReviewRepository userReviewRepository;
    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final AuctionRepository auctionRepository;
    private final NotificationEventService notificationEventService;

    public ReviewService(
            UserReviewRepository userReviewRepository,
            UserRepository userRepository,
            TradeRepository tradeRepository,
            AuctionRepository auctionRepository,
            NotificationEventService notificationEventService
    ) {
        this.userReviewRepository = userReviewRepository;
        this.userRepository = userRepository;
        this.tradeRepository = tradeRepository;
        this.auctionRepository = auctionRepository;
        this.notificationEventService = notificationEventService;
    }

    @Transactional
    @CacheEvict(cacheNames = {"productList", "auctionList"}, allEntries = true)
    public ReviewResponse create(ReviewCreateRequest request, String currentUserEmail) {
        User reviewer = findUserByEmail(currentUserEmail);
        User target = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        ReviewSourceType sourceType = parseSourceType(request.sourceType());

        if (reviewer.getId().equals(target.getId())) {
            throw new JmarketException(ErrorCode.REVIEW_INVALID_TARGET);
        }
        if (userReviewRepository.existsByReviewerIdAndSourceTypeAndSourceId(reviewer.getId(), sourceType, request.sourceId())) {
            throw new JmarketException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        String sourceTitle = validateReviewableSource(sourceType, request.sourceId(), reviewer, target);
        UserReview saved = userReviewRepository.save(new UserReview(
                reviewer,
                target,
                sourceType,
                request.sourceId(),
                request.rating(),
                request.content().trim()
        ));
        notificationEventService.notifyReviewReceived(saved, sourceTitle);
        return ReviewResponse.from(saved, sourceTitle);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(Long userId) {
        return userReviewRepository.findAllByTargetUserIdOrderByCreatedAtDesc(userId).stream()
                .map(review -> ReviewResponse.from(review, resolveSourceTitle(review.getSourceType(), review.getSourceId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(Long userId) {
        List<UserReview> reviews = userReviewRepository.findAllByTargetUserIdOrderByCreatedAtDesc(userId);
        double average = reviews.stream().mapToInt(UserReview::getRating).average().orElse(0.0d);
        double temperature = reviews.isEmpty() ? 36.5d : 36.5d + ((average - 3.0d) * 3.0d);
        return new ReviewSummaryResponse(
                userId,
                reviews.size(),
                Math.round(average * 10.0d) / 10.0d,
                Math.round(temperature * 10.0d) / 10.0d
        );
    }

    private String validateReviewableSource(ReviewSourceType sourceType, Long sourceId, User reviewer, User target) {
        if (sourceType == ReviewSourceType.TRADE) {
            Trade trade = tradeRepository.findById(sourceId)
                    .orElseThrow(() -> new JmarketException(ErrorCode.REVIEW_SOURCE_NOT_FOUND));
            if (trade.getStatus() != TradeStatus.COMPLETED) {
                throw new JmarketException(ErrorCode.TRADE_INVALID_STATUS);
            }
            boolean buyerReviewsSeller = trade.getBuyer().getId().equals(reviewer.getId())
                    && trade.getSeller().getId().equals(target.getId());
            boolean sellerReviewsBuyer = trade.getSeller().getId().equals(reviewer.getId())
                    && trade.getBuyer().getId().equals(target.getId());
            if (!buyerReviewsSeller && !sellerReviewsBuyer) {
                throw new JmarketException(ErrorCode.REVIEW_INVALID_TARGET);
            }
            return trade.getProduct().getTitle();
        }

        Auction auction = auctionRepository.findById(sourceId)
                .orElseThrow(() -> new JmarketException(ErrorCode.REVIEW_SOURCE_NOT_FOUND));
        if (auction.getStatus() != AuctionStatus.CLOSED || auction.getWinnerUser() == null) {
            throw new JmarketException(ErrorCode.AUCTION_NOT_OPEN);
        }
        boolean winnerReviewsSeller = auction.getWinnerUser().getId().equals(reviewer.getId())
                && auction.getSeller().getId().equals(target.getId());
        boolean sellerReviewsWinner = auction.getSeller().getId().equals(reviewer.getId())
                && auction.getWinnerUser().getId().equals(target.getId());
        if (!winnerReviewsSeller && !sellerReviewsWinner) {
            throw new JmarketException(ErrorCode.REVIEW_INVALID_TARGET);
        }
        return auction.getProduct().getTitle();
    }

    private String resolveSourceTitle(ReviewSourceType sourceType, Long sourceId) {
        if (sourceType == ReviewSourceType.TRADE) {
            return tradeRepository.findById(sourceId)
                    .map(trade -> trade.getProduct().getTitle())
                    .orElse("삭제된 거래");
        }
        return auctionRepository.findById(sourceId)
                .map(auction -> auction.getProduct().getTitle())
                .orElse("삭제된 경매");
    }

    private ReviewSourceType parseSourceType(String sourceType) {
        try {
            return ReviewSourceType.valueOf(sourceType.trim().toUpperCase());
        } catch (Exception ex) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }
}
