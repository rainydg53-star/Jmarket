package com.jmarket.trade.service;

import com.jmarket.auth.domain.User;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.mileage.service.MileageService;
import com.jmarket.notification.service.NotificationEventService;
import com.jmarket.product.domain.Product;
import com.jmarket.product.domain.ProductListingType;
import com.jmarket.product.repository.ProductRepository;
import com.jmarket.review.domain.ReviewSourceType;
import com.jmarket.review.domain.UserReview;
import com.jmarket.review.repository.UserReviewRepository;
import com.jmarket.trade.domain.Trade;
import com.jmarket.trade.domain.TradePaymentMethod;
import com.jmarket.trade.domain.TradeStatus;
import com.jmarket.trade.dto.TradeCreateRequest;
import com.jmarket.trade.dto.TradeListRole;
import com.jmarket.trade.dto.TradeResponse;
import com.jmarket.trade.repository.TradeRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeService {

    private static final Set<TradeStatus> ACTIVE_STATUSES = EnumSet.of(TradeStatus.REQUESTED, TradeStatus.ACCEPTED);

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final MileageService mileageService;
    private final NotificationEventService notificationEventService;
    private final UserReviewRepository userReviewRepository;

    public TradeService(
            TradeRepository tradeRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            MileageService mileageService,
            NotificationEventService notificationEventService,
            UserReviewRepository userReviewRepository
    ) {
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.mileageService = mileageService;
        this.notificationEventService = notificationEventService;
        this.userReviewRepository = userReviewRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = "productList", allEntries = true)
    public TradeResponse create(TradeCreateRequest request, String currentUserEmail) {
        User buyer = findUserByEmail(currentUserEmail);
        Product product = findProductById(request.productId());
        User seller = product.getSeller();

        if (buyer.getId().equals(seller.getId())) {
            throw new JmarketException(ErrorCode.TRADE_SELF_NOT_ALLOWED);
        }

        if (tradeRepository.existsByProductIdAndStatusIn(product.getId(), ACTIVE_STATUSES)) {
            throw new JmarketException(ErrorCode.TRADE_ALREADY_IN_PROGRESS);
        }
        if (product.isSold()) {
            throw new JmarketException(ErrorCode.TRADE_INVALID_STATUS);
        }

        Long fixedTradeAmount = product.getPrice();
        Long reservedMileageAmount = fixedTradeAmount;
        Trade trade = new Trade(
                product,
                buyer,
                seller,
                fixedTradeAmount,
                TradePaymentMethod.MILEAGE,
                reservedMileageAmount
        );
        // 마일리지 예약에 성공하면 즉시 거래중(ACCEPTED) 상태로 시작한다.
        trade.accept();
        Trade savedTrade = tradeRepository.save(trade);
        if (savedTrade.getReservedMileageAmount() > 0) {
            mileageService.reserveForTrade(savedTrade.getBuyer().getId(), savedTrade.getReservedMileageAmount(), savedTrade.getId());
        }
        notificationEventService.notifyTradeRequested(savedTrade);
        return toResponse(savedTrade, buyer);
    }

    @Transactional(readOnly = true)
    public TradeResponse getById(Long tradeId, String currentUserEmail) {
        Trade trade = findTradeById(tradeId);
        User currentUser = findUserByEmail(currentUserEmail);
        validateParticipant(trade, currentUser);
        return toResponse(trade, currentUser);
    }

    @Transactional(readOnly = true)
    public List<TradeResponse> getMyTrades(TradeListRole role, String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);

        if (role == TradeListRole.BUYER) {
            return tradeRepository.findAllByBuyerIdOrderByRequestedAtDesc(currentUser.getId()).stream()
                    .map(trade -> toResponse(trade, currentUser))
                    .toList();
        }
        if (role == TradeListRole.SELLER) {
            return tradeRepository.findAllBySellerIdOrderByRequestedAtDesc(currentUser.getId()).stream()
                    .map(trade -> toResponse(trade, currentUser))
                    .toList();
        }

        List<Trade> merged = new ArrayList<>();
        merged.addAll(tradeRepository.findAllByBuyerIdOrderByRequestedAtDesc(currentUser.getId()));
        merged.addAll(tradeRepository.findAllBySellerIdOrderByRequestedAtDesc(currentUser.getId()));
        return merged.stream()
                .sorted((a, b) -> b.getRequestedAt().compareTo(a.getRequestedAt()))
                .map(trade -> toResponse(trade, currentUser))
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "productList", allEntries = true)
    public TradeResponse accept(Long tradeId, String currentUserEmail) {
        Trade trade = findTradeById(tradeId);
        User currentUser = findUserByEmail(currentUserEmail);
        validateSeller(trade, currentUser);

        if (trade.getStatus() != TradeStatus.REQUESTED) {
            throw new JmarketException(ErrorCode.TRADE_INVALID_STATUS);
        }

        trade.accept();
        return toResponse(trade, currentUser);
    }

    @Transactional
    @CacheEvict(cacheNames = "productList", allEntries = true)
    public TradeResponse complete(Long tradeId, String currentUserEmail) {
        Trade trade = findTradeById(tradeId);
        User currentUser = findUserByEmail(currentUserEmail);
        validateParticipant(trade, currentUser);

        if (trade.getStatus() != TradeStatus.ACCEPTED) {
            throw new JmarketException(ErrorCode.TRADE_INVALID_STATUS);
        }

        boolean isBuyer = trade.getBuyer().getId().equals(currentUser.getId());
        boolean confirmed;
        if (isBuyer) {
            confirmed = trade.confirmBuyerReceived();
        } else {
            confirmed = trade.confirmSellerHanded();
        }

        if (!confirmed) {
            throw new JmarketException(ErrorCode.TRADE_INVALID_STATUS);
        }

        if (trade.isBothSidesConfirmed()) {
            if (trade.getReservedMileageAmount() > 0) {
                mileageService.settleTradeReservedTransfer(
                        trade.getBuyer().getId(),
                        trade.getSeller().getId(),
                        trade.getReservedMileageAmount(),
                        trade.getId()
                );
            }
            trade.getProduct().markSold();
            trade.complete();
            notificationEventService.notifyTradeCompleted(trade);
        }
        return toResponse(trade, currentUser);
    }

    @Transactional
    @CacheEvict(cacheNames = "productList", allEntries = true)
    public TradeResponse cancel(Long tradeId, String currentUserEmail) {
        Trade trade = findTradeById(tradeId);
        User currentUser = findUserByEmail(currentUserEmail);
        validateParticipant(trade, currentUser);

        if (trade.getStatus() == TradeStatus.COMPLETED || trade.getStatus() == TradeStatus.CANCELED) {
            throw new JmarketException(ErrorCode.TRADE_INVALID_STATUS);
        }

        if (trade.getReservedMileageAmount() > 0) {
            mileageService.releaseTradeReservation(
                    trade.getBuyer().getId(),
                    trade.getReservedMileageAmount(),
                    trade.getId()
            );
        }
        trade.cancel();
        return toResponse(trade, currentUser);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }

    private Product findProductById(Long productId) {
        return productRepository.findByIdAndListingType(productId, ProductListingType.DIRECT)
                .orElseThrow(() -> new JmarketException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Trade findTradeById(Long tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new JmarketException(ErrorCode.TRADE_NOT_FOUND));
    }

    private void validateSeller(Trade trade, User currentUser) {
        if (!trade.getSeller().getId().equals(currentUser.getId())) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateParticipant(Trade trade, User currentUser) {
        boolean isBuyer = trade.getBuyer().getId().equals(currentUser.getId());
        boolean isSeller = trade.getSeller().getId().equals(currentUser.getId());
        if (!isBuyer && !isSeller) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
    }

    private TradeResponse toResponse(Trade trade, User currentUser) {
        UserReview review = userReviewRepository
                .findByReviewerIdAndSourceTypeAndSourceId(currentUser.getId(), ReviewSourceType.TRADE, trade.getId())
                .orElse(null);
        return TradeResponse.from(trade, currentUser.getId(), review != null, review != null ? review.getId() : null);
    }
}
