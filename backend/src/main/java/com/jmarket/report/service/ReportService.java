package com.jmarket.report.service;

import com.jmarket.auth.domain.User;
import com.jmarket.auth.domain.UserRole;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.auction.repository.AuctionRepository;
import com.jmarket.chat.domain.ChatRoom;
import com.jmarket.chat.repository.ChatRoomRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.product.domain.Product;
import com.jmarket.product.repository.ProductFavoriteRepository;
import com.jmarket.product.repository.ProductImageRepository;
import com.jmarket.product.repository.ProductQuestionRepository;
import com.jmarket.product.repository.ProductRepository;
import com.jmarket.product.repository.ProductViewRepository;
import com.jmarket.notification.service.NotificationEventService;
import com.jmarket.report.domain.Report;
import com.jmarket.report.domain.ReportResolutionAction;
import com.jmarket.report.domain.ReportStatus;
import com.jmarket.report.domain.ReportTargetType;
import com.jmarket.report.dto.ReportCreateRequest;
import com.jmarket.report.dto.ReportResolveRequest;
import com.jmarket.report.dto.ReportResponse;
import com.jmarket.report.repository.ReportRepository;
import com.jmarket.search.ProductSearchService;
import com.jmarket.trade.domain.Trade;
import com.jmarket.trade.repository.TradeRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductQuestionRepository productQuestionRepository;
    private final ProductFavoriteRepository productFavoriteRepository;
    private final ProductViewRepository productViewRepository;
    private final AuctionRepository auctionRepository;
    private final TradeRepository tradeRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final NotificationEventService notificationEventService;

    public ReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ProductQuestionRepository productQuestionRepository,
            ProductFavoriteRepository productFavoriteRepository,
            ProductViewRepository productViewRepository,
            AuctionRepository auctionRepository,
            TradeRepository tradeRepository,
            ChatRoomRepository chatRoomRepository,
            NotificationEventService notificationEventService,
            ObjectProvider<ProductSearchService> productSearchServiceProvider
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productQuestionRepository = productQuestionRepository;
        this.productFavoriteRepository = productFavoriteRepository;
        this.productViewRepository = productViewRepository;
        this.auctionRepository = auctionRepository;
        this.tradeRepository = tradeRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.notificationEventService = notificationEventService;
        this.productSearchService = productSearchServiceProvider.getIfAvailable();
    }

    private final ProductSearchService productSearchService;

    @Transactional
    public ReportResponse create(ReportCreateRequest request, String currentUserEmail) {
        User reporter = findUserByEmail(currentUserEmail);
        validateCreateRequest(request);
        validateTargetExists(request.targetType(), request.targetId());

        Report report = new Report(
                reporter,
                request.targetType(),
                request.targetId(),
                request.reason().trim(),
                request.detail().trim()
        );
        return toResponse(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports(String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        return reportRepository.findAllByReporterIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(Long reportId, String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        Report report = findReportById(reportId);

        if (user.getRole() != UserRole.ADMIN && !report.getReporter().getId().equals(user.getId())) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getAllForAdmin(String currentUserEmail) {
        User admin = findUserByEmail(currentUserEmail);
        validateAdmin(admin);

        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"productList", "auctionList"}, allEntries = true)
    public ReportResponse resolveByAdmin(Long reportId, ReportResolveRequest request, String currentUserEmail) {
        User admin = findUserByEmail(currentUserEmail);
        validateAdmin(admin);
        Report report = findReportById(reportId);

        if (request.status() == ReportStatus.PENDING) {
            throw new JmarketException(ErrorCode.REPORT_INVALID_STATUS);
        }
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new JmarketException(ErrorCode.REPORT_INVALID_STATUS);
        }

        String memo = request.resolutionMemo() != null ? request.resolutionMemo().trim() : null;
        report.resolve(admin, request.status(), request.resolutionAction(), memo);
        applyResolutionAction(report);
        notificationEventService.notifyReportResolved(report);
        return toResponse(report);
    }

    private void applyResolutionAction(Report report) {
        if (report.getStatus() != ReportStatus.RESOLVED) {
            return;
        }
        switch (report.getResolutionAction()) {
            case PRODUCT_REMOVED -> removeReportedProduct(report);
            case TEMP_SUSPEND -> banReportedUser(report, LocalDateTime.now().plusDays(7));
            case PERMANENT_BAN -> banReportedUser(report, null);
            case NONE, WARNING -> {
            }
        }
    }

    private void removeReportedProduct(Report report) {
        if (report.getTargetType() != ReportTargetType.PRODUCT) {
            throw new JmarketException(ErrorCode.REPORT_INVALID_REQUEST);
        }
        Product product = productRepository.findById(report.getTargetId())
                .orElseThrow(() -> new JmarketException(ErrorCode.PRODUCT_NOT_FOUND));
        validateDeletableProduct(product.getId());
        productImageRepository.deleteAllByProductId(product.getId());
        productQuestionRepository.deleteAllByProductId(product.getId());
        productFavoriteRepository.deleteAllByProductId(product.getId());
        productViewRepository.deleteAllByProductId(product.getId());
        productRepository.delete(product);
        deleteSearchIndex(product.getId());
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

    private void banReportedUser(Report report, LocalDateTime bannedUntil) {
        User target = resolveReportedUser(report);
        String reason = report.getResolutionMemo() == null || report.getResolutionMemo().isBlank()
                ? "신고 처리에 따른 이용 제한"
                : report.getResolutionMemo();
        target.ban(bannedUntil, reason);
        notificationEventService.notifyUserRestricted(target.getId(), reason, "/notifications");
    }

    private User resolveReportedUser(Report report) {
        return switch (report.getTargetType()) {
            case USER -> userRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
            case PRODUCT -> productRepository.findById(report.getTargetId())
                    .map(Product::getSeller)
                    .orElseThrow(() -> new JmarketException(ErrorCode.PRODUCT_NOT_FOUND));
            case TRADE -> resolveTradeCounterparty(report);
            case CHAT_ROOM -> resolveChatCounterparty(report);
        };
    }

    private User resolveTradeCounterparty(Report report) {
        Trade trade = tradeRepository.findById(report.getTargetId())
                .orElseThrow(() -> new JmarketException(ErrorCode.TRADE_NOT_FOUND));
        Long reporterId = report.getReporter().getId();
        if (trade.getBuyer().getId().equals(reporterId)) {
            return trade.getSeller();
        }
        if (trade.getSeller().getId().equals(reporterId)) {
            return trade.getBuyer();
        }
        return trade.getSeller();
    }

    private User resolveChatCounterparty(Report report) {
        ChatRoom room = chatRoomRepository.findById(report.getTargetId())
                .orElseThrow(() -> new JmarketException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        Long reporterId = report.getReporter().getId();
        if (room.getParticipantA().getId().equals(reporterId)) {
            return room.getParticipantB();
        }
        if (room.getParticipantB().getId().equals(reporterId)) {
            return room.getParticipantA();
        }
        return room.getParticipantB();
    }

    private void validateCreateRequest(ReportCreateRequest request) {
        if (request.targetId() == null || request.targetId() <= 0) {
            throw new JmarketException(ErrorCode.REPORT_INVALID_REQUEST);
        }
    }

    private void validateTargetExists(ReportTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case PRODUCT -> productRepository.existsById(targetId);
            case TRADE -> tradeRepository.existsById(targetId);
            case CHAT_ROOM -> chatRoomRepository.existsById(targetId);
            case USER -> userRepository.existsById(targetId);
        };

        if (!exists) {
            throw new JmarketException(ErrorCode.REPORT_TARGET_NOT_FOUND);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }

    private Report findReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new JmarketException(ErrorCode.REPORT_NOT_FOUND));
    }

    private void validateAdmin(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
    }

    private ReportResponse toResponse(Report report) {
        long targetReportCount = reportRepository.countByTargetTypeAndTargetId(report.getTargetType(), report.getTargetId());
        return switch (report.getTargetType()) {
            case PRODUCT -> productRepository.findById(report.getTargetId())
                    .map(product -> ReportResponse.from(
                            report,
                            "상품: " + product.getTitle(),
                            product.getSeller().getId(),
                            product.getSeller().getNickname(),
                            targetReportCount,
                            countUserReports(product.getSeller().getId())
                    ))
                    .orElseGet(() -> ReportResponse.from(report, "삭제된 상품 #" + report.getTargetId(), null, null, targetReportCount, 0L));
            case USER -> userRepository.findById(report.getTargetId())
                    .map(user -> ReportResponse.from(
                            report,
                            "사용자: " + user.getNickname(),
                            user.getId(),
                            user.getNickname(),
                            targetReportCount,
                            targetReportCount
                    ))
                    .orElseGet(() -> ReportResponse.from(report, "삭제된 사용자 #" + report.getTargetId(), null, null, targetReportCount, 0L));
            case TRADE -> tradeRepository.findById(report.getTargetId())
                    .map(trade -> ReportResponse.from(
                            report,
                            "거래: " + trade.getProduct().getTitle()
                                    + " / 구매자 " + trade.getBuyer().getNickname()
                                    + " / 판매자 " + trade.getSeller().getNickname(),
                            resolveTradeCounterparty(report).getId(),
                            resolveTradeCounterparty(report).getNickname(),
                            targetReportCount,
                            countUserReports(resolveTradeCounterparty(report).getId())
                    ))
                    .orElseGet(() -> ReportResponse.from(report, "삭제된 거래 #" + report.getTargetId(), null, null, targetReportCount, 0L));
            case CHAT_ROOM -> chatRoomRepository.findById(report.getTargetId())
                    .map(room -> ReportResponse.from(
                            report,
                            "채팅방: " + room.getRoomType().name()
                                    + " / " + room.getParticipantA().getNickname()
                                    + " - " + room.getParticipantB().getNickname(),
                            resolveChatCounterparty(report).getId(),
                            resolveChatCounterparty(report).getNickname(),
                            targetReportCount,
                            countUserReports(resolveChatCounterparty(report).getId())
                    ))
                    .orElseGet(() -> ReportResponse.from(report, "삭제된 채팅방 #" + report.getTargetId(), null, null, targetReportCount, 0L));
        };
    }

    private long countUserReports(Long userId) {
        return reportRepository.countByTargetTypeAndTargetId(ReportTargetType.USER, userId);
    }
}
