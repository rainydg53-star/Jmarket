package com.jmarket.auction.service;

import com.jmarket.admin.domain.AdminCategory;
import com.jmarket.admin.domain.UserRestrictionType;
import com.jmarket.admin.repository.AdminCategoryRepository;
import com.jmarket.admin.service.UserRestrictionService;
import com.jmarket.auction.domain.Auction;
import com.jmarket.auction.domain.AuctionStatus;
import com.jmarket.auction.domain.Bid;
import com.jmarket.auction.dto.AuctionBidEventResponse;
import com.jmarket.auction.dto.AuctionCreateRequest;
import com.jmarket.auction.dto.AuctionBidSnapshot;
import com.jmarket.auction.dto.AuctionResponse;
import com.jmarket.auction.dto.BidRequest;
import com.jmarket.auction.dto.BidResponse;
import com.jmarket.auction.dto.RedisBidResult;
import com.jmarket.auction.repository.AuctionRepository;
import com.jmarket.auction.repository.BidRepository;
import com.jmarket.auth.domain.User;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.mileage.service.MileageService;
import com.jmarket.notification.service.NotificationEventService;
import com.jmarket.product.domain.Product;
import com.jmarket.product.domain.ProductCategory;
import com.jmarket.product.domain.ProductImage;
import com.jmarket.product.domain.ProductListingType;
import com.jmarket.product.dto.ProductImageRequest;
import com.jmarket.product.dto.ProductImageResponse;
import com.jmarket.product.repository.ProductImageRepository;
import com.jmarket.product.repository.ProductRepository;
import com.jmarket.search.ProductSearchService;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final AdminCategoryRepository categoryRepository;
    private final UserRestrictionService restrictionService;
    private final AuctionBidRedisService auctionBidRedisService;
    private final MileageService mileageService;
    private final NotificationEventService notificationEventService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProductSearchService productSearchService;
    private final long closedVisibleHours;

    public AuctionService(
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            AdminCategoryRepository categoryRepository,
            UserRestrictionService restrictionService,
            AuctionBidRedisService auctionBidRedisService,
            MileageService mileageService,
            NotificationEventService notificationEventService,
            SimpMessagingTemplate messagingTemplate,
            ObjectProvider<ProductSearchService> productSearchServiceProvider,
            @Value("${auction.closed-visible-hours:24}") long closedVisibleHours
    ) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.restrictionService = restrictionService;
        this.auctionBidRedisService = auctionBidRedisService;
        this.mileageService = mileageService;
        this.notificationEventService = notificationEventService;
        this.messagingTemplate = messagingTemplate;
        this.productSearchService = productSearchServiceProvider.getIfAvailable();
        this.closedVisibleHours = closedVisibleHours;
    }

    @Transactional
    @CacheEvict(cacheNames = {"auctionList", "productList"}, allEntries = true)
    public AuctionResponse create(AuctionCreateRequest request, String currentUserEmail) {
        User seller = findUserByEmail(currentUserEmail);
        restrictionService.validateAllowed(seller.getId(), UserRestrictionType.AUCTION_CREATE);
        Product product = resolveAuctionProduct(request, seller);

        if (!request.startAt().isBefore(request.endAt())) {
            throw new JmarketException(ErrorCode.AUCTION_INVALID_TIME);
        }
        if (product.isSold()) {
            throw new JmarketException(ErrorCode.TRADE_INVALID_STATUS);
        }
        if (request.instantBuyPrice() != null && request.instantBuyPrice() < request.startPrice()) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }
        if (auctionRepository.existsByProductIdAndStatus(product.getId(), AuctionStatus.OPEN)) {
            throw new JmarketException(ErrorCode.AUCTION_ALREADY_EXISTS);
        }

        Auction auction = new Auction(
                product,
                seller,
                request.startPrice(),
                request.instantBuyPrice(),
                request.startAt(),
                request.endAt()
        );
        Auction savedAuction = auctionRepository.save(auction);
        auctionBidRedisService.initialize(savedAuction);
        if (request.productId() == null) {
            saveImages(product, request.images());
        }
        indexProduct(product);
        return toAuctionResponse(savedAuction);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "auctionList",
            key = "T(java.util.Objects).toString(#keyword, '') + '|' + T(java.util.Objects).toString(#category, '') + '|' + T(java.util.Objects).toString(#sort, '')"
    )
    public List<AuctionResponse> getOpenAuctions(String keyword, String category, String sort) {
        Instant cutoff = Instant.now().minus(closedVisibleHours, ChronoUnit.HOURS);
        String selectedCategory = category == null || category.isBlank() ? null : normalizeCategoryCode(category);

        return auctionRepository.searchVisibleAuctions(keyword, selectedCategory, sort, cutoff).stream()
                .map(this::toAuctionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getMyWonAuctions(String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        return auctionRepository.findAllByStatusAndWinnerUserIdOrderByClosedAtDesc(
                        AuctionStatus.CLOSED,
                        currentUser.getId()
                ).stream()
                .map(this::toAuctionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuctionResponse getById(Long auctionId) {
        Auction auction = findAuctionById(auctionId);
        return toAuctionResponse(auction);
    }

    @Transactional
    @CacheEvict(cacheNames = {"auctionList", "productList"}, allEntries = true)
    public BidResponse placeBid(Long auctionId, BidRequest request, String currentUserEmail) {
        Auction auction = findAuctionById(auctionId);
        User bidder = findUserByEmail(currentUserEmail);
        restrictionService.validateAllowed(bidder.getId(), UserRestrictionType.AUCTION_BID);

        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new JmarketException(ErrorCode.AUCTION_NOT_OPEN);
        }
        Instant now = Instant.now();
        if (now.isBefore(auction.getStartAt()) || !now.isBefore(auction.getEndAt())) {
            throw new JmarketException(ErrorCode.AUCTION_NOT_OPEN);
        }
        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new JmarketException(ErrorCode.AUCTION_SELF_BID_NOT_ALLOWED);
        }

        AuctionBidSnapshot beforeBid = auctionBidRedisService.getSnapshot(auction);
        if (beforeBid.currentHighestBidderId() != null && beforeBid.currentHighestBidderId().equals(bidder.getId())) {
            throw new JmarketException(ErrorCode.AUCTION_ALREADY_TOP_BIDDER);
        }
        long currentPrice = beforeBid.currentHighestBid();
        long minimumAllowed = calculateMinimumAllowed(currentPrice);
        long requestAmount = request.amount() != null ? request.amount() : minimumAllowed;
        boolean instantBuyTriggered = auction.getInstantBuyPrice() != null
                && requestAmount >= auction.getInstantBuyPrice();
        long effectiveBidAmount = instantBuyTriggered ? auction.getInstantBuyPrice() : requestAmount;

        if (!instantBuyTriggered && requestAmount < minimumAllowed) {
            throw new JmarketException(ErrorCode.AUCTION_BID_TOO_LOW);
        }

        mileageService.reserveForAuction(bidder.getId(), effectiveBidAmount, auctionId);
        RedisBidResult bidResult = auctionBidRedisService.placeBid(auction, bidder, requestAmount, auction.getInstantBuyPrice());
        if (!bidResult.accepted()) {
            mileageService.releaseAuctionReservation(bidder.getId(), effectiveBidAmount, auctionId);
            if (bidResult.topBidderAlready()) {
                throw new JmarketException(ErrorCode.AUCTION_ALREADY_TOP_BIDDER);
            }
            throw new JmarketException(ErrorCode.AUCTION_BID_TOO_LOW);
        }
        if (bidResult.previousHighestBidderId() != null) {
            mileageService.releaseAuctionReservation(
                    bidResult.previousHighestBidderId(),
                    bidResult.previousHighestBid(),
                    auctionId
            );
        }

        Bid previousTopBid = bidResult.previousHighestBidderId() == null
                ? null
                : bidRepository.findTopByAuctionIdAndBidderIdOrderByAmountDescBidAtAsc(
                        auctionId,
                        bidResult.previousHighestBidderId()
                ).orElse(null);
        Bid bid = new Bid(auction, bidder, bidResult.effectiveBidAmount());
        Bid savedBid = bidRepository.save(bid);

        if (instantBuyTriggered) {
            mileageService.settleAuctionReservedTransfer(
                    bidder.getId(),
                    auction.getSeller().getId(),
                    bidResult.effectiveBidAmount(),
                    auctionId
            );
            auction.getProduct().markSold();
            auction.close(bidder, bidResult.effectiveBidAmount());
            notificationEventService.notifyAuctionWon(auction, savedBid);
        }
        if (previousTopBid != null) {
            notificationEventService.notifyAuctionOutbidLost(auction, previousTopBid, savedBid);
        }
        notificationEventService.notifyAuctionOutbid(auction, savedBid);
        publishBidPlaced(auction, savedBid);
        return BidResponse.from(savedBid);
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getBids(Long auctionId) {
        findAuctionById(auctionId);
        return bidRepository.findAllByAuctionIdOrderByBidAtAsc(auctionId).stream()
                .map(BidResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"auctionList", "productList"}, allEntries = true)
    public AuctionResponse close(Long auctionId, String currentUserEmail) {
        Auction auction = findAuctionByIdForUpdate(auctionId);
        User currentUser = findUserByEmail(currentUserEmail);

        if (!auction.getSeller().getId().equals(currentUser.getId())) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new JmarketException(ErrorCode.AUCTION_NOT_OPEN);
        }
        if (Instant.now().isBefore(auction.getEndAt())) {
            throw new JmarketException(ErrorCode.AUCTION_NOT_ENDED);
        }

        closeAuctionAndSettle(auction);
        return toAuctionResponse(auction);
    }

    @Transactional
    @CacheEvict(cacheNames = {"auctionList", "productList"}, allEntries = true)
    public int closeExpiredOpenAuctions() {
        List<Auction> targets = auctionRepository.findAllByStatusAndEndAtLessThanEqualOrderByEndAtAsc(
                AuctionStatus.OPEN,
                Instant.now()
        );
        Instant now = Instant.now();
        for (Auction target : targets) {
            Auction auction = findAuctionByIdForUpdate(target.getId());
            if (auction.getStatus() != AuctionStatus.OPEN) {
                continue;
            }
            if (auction.getEndAt().isAfter(now)) {
                continue;
            }
            closeAuctionAndSettle(auction);
        }
        return targets.size();
    }

    private void closeAuctionAndSettle(Auction auction) {
        AuctionBidSnapshot snapshot = auctionBidRedisService.getSnapshot(auction);
        if (snapshot.currentHighestBidderId() == null) {
            auction.close(null, null);
        } else {
            mileageService.settleAuctionReservedTransfer(
                    snapshot.currentHighestBidderId(),
                    auction.getSeller().getId(),
                    snapshot.currentHighestBid(),
                    auction.getId()
            );
            auction.getProduct().markSold();
            User winner = userRepository.findById(snapshot.currentHighestBidderId())
                    .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
            auction.close(winner, snapshot.currentHighestBid());
            bidRepository.findTopByAuctionIdOrderByAmountDescBidAtAsc(auction.getId())
                    .ifPresent(topBid -> notificationEventService.notifyAuctionWon(auction, topBid));
        }
    }

    private AuctionResponse toAuctionResponse(Auction auction) {
        AuctionBidSnapshot snapshot = auctionBidRedisService.getSnapshot(auction);
        return AuctionResponse.from(
                auction,
                snapshot.currentHighestBid(),
                snapshot.currentHighestBidderId(),
                snapshot.currentHighestBidderNickname(),
                snapshot.totalBidCount(),
                productImageRepository.findAllByProductIdOrderBySortOrderAsc(auction.getProduct().getId()).stream()
                        .map(ProductImageResponse::from)
                        .toList()
                ,
                resolveCategoryLabel(auction.getProduct().getCategoryCode())
        );
    }

    private void publishBidPlaced(Auction auction, Bid bid) {
        AuctionResponse auctionResponse = toAuctionResponse(auction);
        BidResponse bidResponse = BidResponse.from(bid);
        messagingTemplate.convertAndSend(
                "/topic/auctions." + auction.getId(),
                AuctionBidEventResponse.bidPlaced(auctionResponse, bidResponse)
        );
    }

    private void saveImages(Product product, List<ProductImageRequest> images) {
        List<ProductImageRequest> safeImages = images == null ? List.of() : images;
        if (safeImages.isEmpty()) {
            return;
        }

        boolean hasThumbnail = safeImages.stream().anyMatch(ProductImageRequest::thumbnail);
        boolean thumbnailAssigned = false;
        List<ProductImage> productImages = new ArrayList<>();
        for (int i = 0; i < safeImages.size(); i++) {
            ProductImageRequest image = safeImages.get(i);
            boolean thumbnail = (image.thumbnail() && !thumbnailAssigned) || (!hasThumbnail && i == 0);
            if (thumbnail) {
                thumbnailAssigned = true;
            }
            productImages.add(new ProductImage(
                    product,
                    image.imageUrl().trim(),
                    thumbnail,
                    i
            ));
        }
        productImageRepository.saveAll(productImages);
    }

    private void indexProduct(Product product) {
        if (productSearchService != null) {
            productSearchService.index(product);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new JmarketException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Product resolveAuctionProduct(AuctionCreateRequest request, User seller) {
        if (request.productId() != null) {
            Product existingProduct = findProductById(request.productId());
            if (!existingProduct.getSeller().getId().equals(seller.getId())) {
                throw new JmarketException(ErrorCode.FORBIDDEN);
            }
            if (existingProduct.getListingType() != ProductListingType.AUCTION) {
                throw new JmarketException(ErrorCode.INVALID_INPUT);
            }
            return existingProduct;
        }

        if (request.title() == null || request.title().isBlank()
                || request.description() == null || request.description().isBlank()) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }

        long productPrice = request.instantBuyPrice() != null
                ? request.instantBuyPrice()
                : request.startPrice();

        Product newProduct = new Product(
                seller,
                request.title().trim(),
                request.description().trim(),
                resolveActiveCategoryCode(request.category()),
                productPrice,
                ProductListingType.AUCTION
        );
        return productRepository.save(newProduct);
    }

    private Comparator<Auction> auctionComparator(String sort) {
        String normalizedSort = sort == null ? "ENDING_SOON" : sort.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedSort) {
            case "LATEST" -> Comparator.comparing(Auction::getCreatedAt).reversed();
            case "PRICE_ASC" -> Comparator.comparing(Auction::getStartPrice).thenComparing(Auction::getEndAt);
            case "PRICE_DESC" -> Comparator.comparing(Auction::getStartPrice).reversed();
            default -> Comparator.comparing(Auction::getEndAt);
        };
    }

    private Auction findAuctionById(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new JmarketException(ErrorCode.AUCTION_NOT_FOUND));
    }

    private Auction findAuctionByIdForUpdate(Long auctionId) {
        return auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new JmarketException(ErrorCode.AUCTION_NOT_FOUND));
    }

    private long calculateMinimumAllowed(long currentPrice) {
        return (long) Math.ceil(currentPrice * 1.1d);
    }

    private String resolveActiveCategoryCode(String rawCategory) {
        String code = normalizeCategoryCode(rawCategory);
        AdminCategory category = categoryRepository.findByCode(code)
                .orElseThrow(() -> new JmarketException(ErrorCode.INVALID_INPUT));
        if (!category.isActive()) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }
        return category.getCode();
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

    private String resolveCategoryLabel(String categoryCode) {
        String code = normalizeCategoryCode(categoryCode);
        return categoryRepository.findByCode(code)
                .map(AdminCategory::getName)
                .orElseGet(() -> ProductCategory.from(code).label());
    }
}
