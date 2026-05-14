package com.jmarket.auth.service;

import com.jmarket.admin.domain.AdminCategory;
import com.jmarket.admin.repository.AdminCategoryRepository;
import com.jmarket.auth.domain.User;
import com.jmarket.auth.dto.UserProfileResponse;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.product.domain.Product;
import com.jmarket.product.domain.ProductCategory;
import com.jmarket.product.domain.ProductImage;
import com.jmarket.product.domain.ProductListingType;
import com.jmarket.product.dto.ProductSummaryResponse;
import com.jmarket.product.repository.ProductImageRepository;
import com.jmarket.product.repository.ProductRepository;
import com.jmarket.review.dto.ReviewResponse;
import com.jmarket.review.repository.UserReviewRepository;
import com.jmarket.trade.domain.TradeStatus;
import com.jmarket.trade.repository.TradeRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final Set<TradeStatus> ACTIVE_TRADE_STATUSES = EnumSet.of(TradeStatus.REQUESTED, TradeStatus.ACCEPTED);

    private final UserRepository userRepository;
    private final UserReviewRepository userReviewRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final AdminCategoryRepository categoryRepository;
    private final TradeRepository tradeRepository;

    public UserProfileService(
            UserRepository userRepository,
            UserReviewRepository userReviewRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            AdminCategoryRepository categoryRepository,
            TradeRepository tradeRepository
    ) {
        this.userRepository = userRepository;
        this.userReviewRepository = userReviewRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.tradeRepository = tradeRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        long reviewCount = userReviewRepository.countByTargetUserId(user.getId());
        double averageRating = roundOne(userReviewRepository.averageRatingByTargetUserId(user.getId()) == null
                ? 0.0d
                : userReviewRepository.averageRatingByTargetUserId(user.getId()));
        double mannerTemperature = roundOne(reviewCount == 0 ? 36.5d : 36.5d + ((averageRating - 3.0d) * 3.0d));

        List<ProductSummaryResponse> sellingProducts = productRepository
                .findAllBySellerIdAndListingTypeOrderByCreatedAtDesc(user.getId(), ProductListingType.DIRECT)
                .stream()
                .map(product -> ProductSummaryResponse.from(
                        product,
                        getThumbnailUrl(product.getId()),
                        resolveCategoryLabel(product.getCategoryCode()),
                        resolveTradeStatus(product),
                        resolveTradeStatusLabel(product),
                        reviewCount,
                        averageRating,
                        mannerTemperature
                ))
                .toList();
        List<ReviewResponse> reviews = userReviewRepository.findAllByTargetUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(ReviewResponse::from)
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                reviewCount,
                averageRating,
                mannerTemperature,
                sellingProducts.size(),
                sellingProducts,
                reviews
        );
    }

    private String getThumbnailUrl(Long productId) {
        return productImageRepository.findAllByProductIdOrderBySortOrderAsc(productId).stream()
                .filter(ProductImage::isThumbnail)
                .findFirst()
                .or(() -> productImageRepository.findAllByProductIdOrderBySortOrderAsc(productId).stream().findFirst())
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    private String resolveCategoryLabel(String categoryCode) {
        String code = normalizeCategoryCode(categoryCode);
        return categoryRepository.findByCode(code)
                .map(AdminCategory::getName)
                .orElseGet(() -> ProductCategory.from(code).label());
    }

    private String normalizeCategoryCode(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return ProductCategory.ETC.name();
        }
        String code = rawCategory.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        if (categoryRepository.existsByCode(code)) {
            return code;
        }
        return ProductCategory.from(rawCategory).name();
    }

    private String resolveTradeStatus(Product product) {
        if (product.isSold()) {
            return "COMPLETED";
        }
        return tradeRepository.findFirstByProductIdAndStatusInOrderByRequestedAtDesc(product.getId(), ACTIVE_TRADE_STATUSES)
                .map(trade -> "RESERVED")
                .orElse("ON_SALE");
    }

    private String resolveTradeStatusLabel(Product product) {
        return switch (resolveTradeStatus(product)) {
            case "COMPLETED" -> "거래완료";
            case "RESERVED" -> "예약중";
            default -> "판매중";
        };
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0d) / 10.0d;
    }
}
