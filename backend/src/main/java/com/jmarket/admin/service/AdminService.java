package com.jmarket.admin.service;

import com.jmarket.admin.domain.AdminCategory;
import com.jmarket.admin.domain.AdminAuditLog;
import com.jmarket.admin.domain.UserRestriction;
import com.jmarket.admin.domain.UserRestrictionType;
import com.jmarket.admin.dto.AdminAuditActionMetricResponse;
import com.jmarket.admin.dto.AdminAuditDailyMetricResponse;
import com.jmarket.admin.dto.AdminAuctionResponse;
import com.jmarket.admin.dto.AdminCategoryRequest;
import com.jmarket.admin.dto.AdminCategoryResponse;
import com.jmarket.admin.dto.AdminCategoryUpdateRequest;
import com.jmarket.admin.dto.AdminDailyMetricResponse;
import com.jmarket.admin.dto.AdminDashboardResponse;
import com.jmarket.admin.dto.AdminProductResponse;
import com.jmarket.admin.dto.AdminRestrictionCreateRequest;
import com.jmarket.admin.dto.AdminRestrictionResponse;
import com.jmarket.admin.dto.AdminUserBanRequest;
import com.jmarket.admin.dto.AdminUserResponse;
import com.jmarket.admin.dto.AdminUserRoleRequest;
import com.jmarket.admin.dto.AdminUserUpdateRequest;
import com.jmarket.admin.repository.AdminCategoryRepository;
import com.jmarket.admin.repository.AdminAuditLogRepository;
import com.jmarket.admin.repository.UserRestrictionRepository;
import com.jmarket.auction.domain.Auction;
import com.jmarket.auction.domain.AuctionStatus;
import com.jmarket.auction.dto.AuctionBidSnapshot;
import com.jmarket.auction.repository.AuctionRepository;
import com.jmarket.auction.service.AuctionBidRedisService;
import com.jmarket.auth.domain.User;
import com.jmarket.auth.domain.UserRole;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.mileage.domain.MileageLedgerType;
import com.jmarket.mileage.domain.MileageLedger;
import com.jmarket.mileage.domain.MileageAccount;
import com.jmarket.mileage.repository.MileageAccountRepository;
import com.jmarket.mileage.repository.MileageLedgerRepository;
import com.jmarket.mileage.service.MileageService;
import com.jmarket.payment.domain.PaymentStatus;
import com.jmarket.payment.repository.PaymentRepository;
import com.jmarket.product.domain.Product;
import com.jmarket.product.domain.ProductCategory;
import com.jmarket.product.domain.ProductListingType;
import com.jmarket.product.repository.ProductFavoriteRepository;
import com.jmarket.product.repository.ProductImageRepository;
import com.jmarket.product.repository.ProductQuestionRepository;
import com.jmarket.product.repository.ProductRepository;
import com.jmarket.product.repository.ProductViewRepository;
import com.jmarket.report.domain.Report;
import com.jmarket.report.repository.ReportRepository;
import com.jmarket.search.ProductSearchService;
import com.jmarket.trade.domain.Trade;
import com.jmarket.trade.domain.TradeStatus;
import com.jmarket.trade.repository.TradeRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AuctionRepository auctionRepository;
    private final TradeRepository tradeRepository;
    private final MileageAccountRepository mileageAccountRepository;
    private final MileageLedgerRepository mileageLedgerRepository;
    private final PaymentRepository paymentRepository;
    private final ReportRepository reportRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductQuestionRepository productQuestionRepository;
    private final ProductFavoriteRepository productFavoriteRepository;
    private final ProductViewRepository productViewRepository;
    private final AdminCategoryRepository categoryRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final UserRestrictionRepository restrictionRepository;
    private final AdminAuditService auditService;
    private final ProductSearchService productSearchService;
    private final AuctionBidRedisService auctionBidRedisService;
    private final MileageService mileageService;

    public AdminService(
            UserRepository userRepository,
            ProductRepository productRepository,
            AuctionRepository auctionRepository,
            TradeRepository tradeRepository,
            MileageAccountRepository mileageAccountRepository,
            MileageLedgerRepository mileageLedgerRepository,
            PaymentRepository paymentRepository,
            ReportRepository reportRepository,
            ProductImageRepository productImageRepository,
            ProductQuestionRepository productQuestionRepository,
            ProductFavoriteRepository productFavoriteRepository,
            ProductViewRepository productViewRepository,
            AdminCategoryRepository categoryRepository,
            AdminAuditLogRepository auditLogRepository,
            UserRestrictionRepository restrictionRepository,
            AdminAuditService auditService,
            AuctionBidRedisService auctionBidRedisService,
            MileageService mileageService,
            ObjectProvider<ProductSearchService> productSearchServiceProvider
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.auctionRepository = auctionRepository;
        this.tradeRepository = tradeRepository;
        this.mileageAccountRepository = mileageAccountRepository;
        this.mileageLedgerRepository = mileageLedgerRepository;
        this.paymentRepository = paymentRepository;
        this.reportRepository = reportRepository;
        this.productImageRepository = productImageRepository;
        this.productQuestionRepository = productQuestionRepository;
        this.productFavoriteRepository = productFavoriteRepository;
        this.productViewRepository = productViewRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.restrictionRepository = restrictionRepository;
        this.auditService = auditService;
        this.auctionBidRedisService = auctionBidRedisService;
        this.mileageService = mileageService;
        this.productSearchService = productSearchServiceProvider.getIfAvailable();
    }

    @PostConstruct
    @Transactional
    public void seedDefaultCategories() {
        int order = 1;
        for (ProductCategory category : ProductCategory.values()) {
            if (!categoryRepository.existsByCode(category.name())) {
                categoryRepository.save(new AdminCategory(category.name(), category.label(), order, true));
            }
            order++;
        }
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long directProducts = productRepository.findAllByListingTypeOrderByCreatedAtDesc(ProductListingType.DIRECT).size();
        long auctionProducts = productRepository.findAllByListingTypeOrderByCreatedAtDesc(ProductListingType.AUCTION).size();
        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countByCreatedAtGreaterThanEqual(todayStart),
                directProducts,
                auctionProducts,
                auctionRepository.countByStatus(AuctionStatus.OPEN),
                tradeRepository.countByStatus(TradeStatus.COMPLETED),
                valueOrZero(mileageLedgerRepository.sumAmountByType(MileageLedgerType.CHARGE)),
                valueOrZero(mileageLedgerRepository.sumAmountByType(MileageLedgerType.USE)),
                valueOrZero(paymentRepository.sumAmountByStatus(PaymentStatus.APPROVED)),
                buildDailyMetrics(),
                buildAuditDailyMetrics(),
                buildAuditActionMetrics()
        );
    }

    private List<AdminAuditDailyMetricResponse> buildAuditDailyMetrics() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        Map<LocalDate, Long> countsByDate = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            countsByDate.put(startDate.plusDays(i), 0L);
        }

        for (AdminAuditLog log : auditLogRepository.findAll()) {
            if (log.getCreatedAt().isBefore(startDateTime)) {
                continue;
            }
            LocalDate date = log.getCreatedAt().toLocalDate();
            countsByDate.put(date, countsByDate.getOrDefault(date, 0L) + 1L);
        }

        List<AdminAuditDailyMetricResponse> responses = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            responses.add(new AdminAuditDailyMetricResponse(date, countsByDate.getOrDefault(date, 0L)));
        }
        return responses;
    }

    private List<AdminAuditActionMetricResponse> buildAuditActionMetrics() {
        LocalDateTime startDateTime = LocalDate.now().minusDays(6).atStartOfDay();
        Map<String, Long> countsByAction = new HashMap<>();
        for (AdminAuditLog log : auditLogRepository.findAll()) {
            if (log.getCreatedAt().isBefore(startDateTime)) {
                continue;
            }
            countsByAction.put(log.getAction(), countsByAction.getOrDefault(log.getAction(), 0L) + 1L);
        }
        return countsByAction.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new AdminAuditActionMetricResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<AdminDailyMetricResponse> buildDailyMetrics() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        Map<LocalDate, DailyMetricAccumulator> dailyMetrics = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            dailyMetrics.put(startDate.plusDays(i), new DailyMetricAccumulator());
        }

        for (User user : userRepository.findAll()) {
            LocalDate createdDate = user.getCreatedAt().toLocalDate();
            if (!createdDate.isBefore(startDate)) {
                dailyMetrics.computeIfAbsent(createdDate, ignored -> new DailyMetricAccumulator())
                        .activeUserIds.add(user.getId());
            }
        }

        for (Trade trade : tradeRepository.findAll()) {
            addTradeActivity(dailyMetrics, startDate, trade.getRequestedAt(), trade.getBuyer().getId(), trade.getSeller().getId());
            if (trade.getCompletedAt() != null && !trade.getCompletedAt().isBefore(startDateTime)) {
                DailyMetricAccumulator metric = dailyMetrics.computeIfAbsent(
                        trade.getCompletedAt().toLocalDate(),
                        ignored -> new DailyMetricAccumulator()
                );
                metric.completedTrades++;
                metric.activeUserIds.add(trade.getBuyer().getId());
                metric.activeUserIds.add(trade.getSeller().getId());
            }
        }

        for (Report report : reportRepository.findAll()) {
            addUserActivity(dailyMetrics, startDate, report.getCreatedAt(), report.getReporter().getId());
            if (report.getProcessedAt() != null && !report.getProcessedAt().isBefore(startDateTime)) {
                DailyMetricAccumulator metric = dailyMetrics.computeIfAbsent(
                        report.getProcessedAt().toLocalDate(),
                        ignored -> new DailyMetricAccumulator()
                );
                metric.processedReports++;
                if (report.getProcessedBy() != null) {
                    metric.activeUserIds.add(report.getProcessedBy().getId());
                }
            }
        }

        for (MileageLedger ledger : mileageLedgerRepository.findAll()) {
            if (ledger.getCreatedAt().isBefore(startDateTime)) {
                continue;
            }
            DailyMetricAccumulator metric = dailyMetrics.computeIfAbsent(
                    ledger.getCreatedAt().toLocalDate(),
                    ignored -> new DailyMetricAccumulator()
            );
            metric.activeUserIds.add(ledger.getUser().getId());
            if (ledger.getType() == MileageLedgerType.CHARGE) {
                metric.mileageCharged += ledger.getAmount();
            } else if (ledger.getType() == MileageLedgerType.USE) {
                metric.mileageUsed += ledger.getAmount();
            }
        }

        List<AdminDailyMetricResponse> responses = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            DailyMetricAccumulator metric = dailyMetrics.get(date);
            responses.add(new AdminDailyMetricResponse(
                    date,
                    metric.activeUserIds.size(),
                    metric.completedTrades,
                    metric.processedReports,
                    metric.mileageCharged,
                    metric.mileageUsed
            ));
        }
        return responses;
    }

    private void addTradeActivity(
            Map<LocalDate, DailyMetricAccumulator> dailyMetrics,
            LocalDate startDate,
            LocalDateTime dateTime,
            Long buyerId,
            Long sellerId
    ) {
        if (dateTime == null || dateTime.toLocalDate().isBefore(startDate)) {
            return;
        }
        DailyMetricAccumulator metric = dailyMetrics.computeIfAbsent(
                dateTime.toLocalDate(),
                ignored -> new DailyMetricAccumulator()
        );
        metric.activeUserIds.add(buyerId);
        metric.activeUserIds.add(sellerId);
    }

    private void addUserActivity(
            Map<LocalDate, DailyMetricAccumulator> dailyMetrics,
            LocalDate startDate,
            LocalDateTime dateTime,
            Long userId
    ) {
        if (dateTime == null || dateTime.toLocalDate().isBefore(startDate)) {
            return;
        }
        dailyMetrics.computeIfAbsent(dateTime.toLocalDate(), ignored -> new DailyMetricAccumulator())
                .activeUserIds.add(userId);
    }

    private static class DailyMetricAccumulator {
        private final Set<Long> activeUserIds = new HashSet<>();
        private long completedTrades;
        private long processedReports;
        private long mileageCharged;
        private long mileageUsed;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers() {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, MileageAccount> accountsByUserId = mileageAccountRepository
                .findAllByUserIdIn(users.stream().map(User::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(account -> account.getUser().getId(), account -> account));
        return users.stream()
                .map(user -> AdminUserResponse.from(user, accountsByUserId.get(user.getId())))
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long userId, AdminUserRoleRequest request, String adminEmail) {
        requireRoleManager(adminEmail);
        User user = findUser(userId);
        UserRole role = parseRole(request.role());
        user.changeRole(role);
        auditService.log(adminEmail, "USER_ROLE_UPDATE", "USER", userId, "role=" + role.name());
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request, String adminEmail) {
        User user = findUser(userId);
        String nickname = requireText(request.nickname(), "nickname");
        if (!nickname.equals(user.getNickname()) && userRepository.existsByNickname(nickname)) {
            throw new JmarketException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        user.changeNickname(nickname);
        user.changeName(trimToNull(request.name()));
        user.changePhoneNumber(trimToNull(request.phoneNumber()));
        UserRole nextRole = parseRole(request.role());
        if (user.getRole() != nextRole) {
            requireRoleManager(adminEmail);
            user.changeRole(nextRole);
            auditService.log(adminEmail, "USER_ROLE_UPDATE", "USER", userId, "role=" + nextRole.name());
        }

        if (Boolean.TRUE.equals(request.banned())) {
            String reason = request.banReason() == null || request.banReason().isBlank()
                    ? "ADMIN_UPDATE"
                    : request.banReason().trim();
            user.ban(request.bannedUntil(), reason);
        } else {
            user.unban();
        }

        auditService.log(adminEmail, "USER_PROFILE_UPDATE", "USER", userId, user.getEmail());
        MileageAccount account = mileageAccountRepository.findByUserId(userId).orElse(null);
        return AdminUserResponse.from(user, account);
    }

    @Transactional
    public AdminUserResponse banUser(Long userId, AdminUserBanRequest request, String adminEmail) {
        User user = findUser(userId);
        String reason = request.reason() == null || request.reason().isBlank() ? "관리자 제재" : request.reason().trim();
        user.ban(request.bannedUntil(), reason);
        auditService.log(adminEmail, "USER_BAN", "USER", userId, reason);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse unbanUser(Long userId, String adminEmail) {
        User user = findUser(userId);
        user.unban();
        auditService.log(adminEmail, "USER_UNBAN", "USER", userId, "제재 해제");
        return AdminUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<AdminRestrictionResponse> getRestrictions() {
        return restrictionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminRestrictionResponse::from)
                .toList();
    }

    @Transactional
    public AdminRestrictionResponse createRestriction(
            Long userId,
            AdminRestrictionCreateRequest request,
            String adminEmail
    ) {
        User user = findUser(userId);
        UserRestrictionType type = parseRestrictionType(request.type());
        String reason = request.reason() == null || request.reason().isBlank()
                ? type.label()
                : request.reason().trim();
        UserRestriction saved = restrictionRepository.save(new UserRestriction(
                user,
                type,
                reason,
                request.restrictedUntil()
        ));
        auditService.log(adminEmail, "USER_FEATURE_RESTRICT", "USER", userId, type.name() + ": " + reason);
        return AdminRestrictionResponse.from(saved);
    }

    @Transactional
    public void deactivateRestriction(Long restrictionId, String adminEmail) {
        UserRestriction restriction = restrictionRepository.findById(restrictionId)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        restriction.deactivate();
        auditService.log(
                adminEmail,
                "USER_FEATURE_RESTRICTION_RELEASE",
                "USER_RESTRICTION",
                restrictionId,
                restriction.getType().name()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(AdminCategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryList", key = "'active'")
    public List<AdminCategoryResponse> getActiveCategories() {
        return categoryRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(AdminCategoryResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"categoryList", "productList", "auctionList"}, allEntries = true)
    public AdminCategoryResponse createCategory(AdminCategoryRequest request, String adminEmail) {
        String code = normalizeCode(request.code());
        if (categoryRepository.existsByCode(code)) {
            throw new JmarketException(ErrorCode.ADMIN_CATEGORY_DUPLICATED);
        }
        AdminCategory saved = categoryRepository.save(new AdminCategory(code, request.name().trim(), request.displayOrder(), request.active()));
        auditService.log(adminEmail, "CATEGORY_CREATE", "CATEGORY", saved.getId(), code);
        return AdminCategoryResponse.from(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"categoryList", "productList", "auctionList"}, allEntries = true)
    public AdminCategoryResponse updateCategory(Long categoryId, AdminCategoryUpdateRequest request, String adminEmail) {
        AdminCategory category = findCategory(categoryId);
        category.update(request.name().trim(), request.displayOrder(), request.active());
        auditService.log(adminEmail, "CATEGORY_UPDATE", "CATEGORY", categoryId, category.getCode());
        return AdminCategoryResponse.from(category);
    }

    @Transactional
    @CacheEvict(cacheNames = {"categoryList", "productList", "auctionList"}, allEntries = true)
    public void deleteCategory(Long categoryId, String adminEmail) {
        AdminCategory category = findCategory(categoryId);
        categoryRepository.delete(category);
        auditService.log(adminEmail, "CATEGORY_DELETE", "CATEGORY", categoryId, category.getCode());
    }

    @Transactional(readOnly = true)
    public List<AdminProductResponse> getProducts() {
        return productRepository.findAllByListingTypeOrderByCreatedAtDesc(ProductListingType.DIRECT).stream()
                .map(product -> AdminProductResponse.from(product, resolveCategoryLabel(product.getCategoryCode())))
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"productList", "auctionList"}, allEntries = true)
    public void deleteProduct(Long productId, String adminEmail) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new JmarketException(ErrorCode.PRODUCT_NOT_FOUND));
        validateDeletableProduct(productId);
        productImageRepository.deleteAllByProductId(productId);
        productQuestionRepository.deleteAllByProductId(productId);
        productFavoriteRepository.deleteAllByProductId(productId);
        productViewRepository.deleteAllByProductId(productId);
        productRepository.delete(product);
        deleteSearchIndex(productId);
        auditService.log(adminEmail, "PRODUCT_DELETE", "PRODUCT", productId, product.getTitle());
    }

    private void validateDeletableProduct(Long productId) {
        if (tradeRepository.existsByProductId(productId) || auctionRepository.existsByProductId(productId)) {
            throw new JmarketException(ErrorCode.PRODUCT_DELETE_NOT_ALLOWED);
        }
    }

    private void deleteSearchIndex(Long productId) {
        if (productSearchService != null) {
            productSearchService.delete(productId);
        }
    }

    @Transactional(readOnly = true)
    public List<AdminAuctionResponse> getAuctions() {
        return auctionRepository.findAll().stream()
                .sorted(Comparator.comparing(Auction::getCreatedAt).reversed())
                .map(AdminAuctionResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"auctionList", "productList"}, allEntries = true)
    public AdminAuctionResponse cancelAuction(Long auctionId, String adminEmail) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new JmarketException(ErrorCode.AUCTION_NOT_FOUND));
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new JmarketException(ErrorCode.AUCTION_NOT_OPEN);
        }
        AuctionBidSnapshot snapshot = auctionBidRedisService.getSnapshot(auction);
        if (snapshot.currentHighestBidderId() != null) {
            mileageService.releaseAuctionReservation(
                    snapshot.currentHighestBidderId(),
                    snapshot.currentHighestBid(),
                    auctionId
            );
        }
        auction.cancel();
        auctionBidRedisService.initialize(auction);
        auditService.log(adminEmail, "AUCTION_CANCEL", "AUCTION", auctionId, auction.getProduct().getTitle());
        return AdminAuctionResponse.from(auction);
    }

    @Transactional
    @CacheEvict(cacheNames = {"auctionList", "productList"}, allEntries = true)
    public void hideAuction(Long auctionId, String adminEmail) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new JmarketException(ErrorCode.AUCTION_NOT_FOUND));
        if (auction.getStatus() != AuctionStatus.CLOSED) {
            throw new JmarketException(ErrorCode.AUCTION_NOT_ENDED);
        }
        auction.hide();
        auditService.log(adminEmail, "AUCTION_HIDE", "AUCTION", auctionId, auction.getProduct().getTitle());
    }

    @Transactional
    @CacheEvict(cacheNames = {"auctionList", "productList"}, allEntries = true)
    public AdminAuctionResponse restoreAuction(Long auctionId, String adminEmail) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new JmarketException(ErrorCode.AUCTION_NOT_FOUND));
        auction.show();
        auditService.log(adminEmail, "AUCTION_RESTORE", "AUCTION", auctionId, auction.getProduct().getTitle());
        return AdminAuctionResponse.from(auction);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }

    private AdminCategory findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new JmarketException(ErrorCode.ADMIN_CATEGORY_NOT_FOUND));
    }

    private UserRole parseRole(String rawRole) {
        try {
            return UserRole.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }
    }

    private void requireRoleManager(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        if (!admin.getRole().canManageRoles()) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
    }

    private UserRestrictionType parseRestrictionType(String rawType) {
        try {
            return UserRestrictionType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolveCategoryLabel(String categoryCode) {
        String code = normalizeCode(categoryCode);
        return categoryRepository.findByCode(code)
                .map(AdminCategory::getName)
                .orElseGet(() -> ProductCategory.from(code).label());
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
