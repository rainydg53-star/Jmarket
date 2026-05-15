package com.jmarket.notification.service;

import com.jmarket.auth.domain.User;
import com.jmarket.auth.domain.UserRole;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.auction.domain.Auction;
import com.jmarket.auction.domain.Bid;
import com.jmarket.chat.domain.ChatMessage;
import com.jmarket.chat.domain.ChatRoom;
import com.jmarket.mileage.domain.MileageWithdrawal;
import com.jmarket.notification.domain.Notification;
import com.jmarket.notification.dto.NotificationEventResponse;
import com.jmarket.notification.dto.NotificationType;
import com.jmarket.report.domain.Report;
import com.jmarket.review.domain.UserReview;
import com.jmarket.trade.domain.Trade;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventService {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final UserRepository userRepository;

    public NotificationEventService(
            NotificationService notificationService,
            NotificationSseService notificationSseService,
            UserRepository userRepository
    ) {
        this.notificationService = notificationService;
        this.notificationSseService = notificationSseService;
        this.userRepository = userRepository;
    }

    public void notifyAuctionOutbid(Auction auction, Bid topBid) {
        Long sellerId = auction.getSeller().getId();
        Notification notification = notificationService.create(
                sellerId,
                NotificationType.AUCTION_OUTBID,
                "내 경매품 상위 입찰",
                "'" + auction.getProduct().getTitle() + "'에 "
                        + topBid.getBidder().getNickname() + "님이 "
                        + topBid.getAmount() + " 마일리지로 입찰했습니다.",
                "/auctions/" + auction.getId()
        );
        notificationSseService.sendToUser(sellerId, toEvent(notification));
    }

    public void notifyAuctionOutbidLost(Auction auction, Bid previousTopBid, Bid newTopBid) {
        Long previousTopBidderId = previousTopBid.getBidder().getId();
        Notification notification = notificationService.create(
                previousTopBidderId,
                NotificationType.AUCTION_OUTBID_LOST,
                "상위 입찰자 변경",
                "'" + auction.getProduct().getTitle() + "' 경매에서 "
                        + newTopBid.getBidder().getNickname() + "님이 "
                        + newTopBid.getAmount() + " 마일리지로 입찰해 최고 입찰자가 변경되었습니다.",
                "/auctions/" + auction.getId()
        );
        notificationSseService.sendToUser(previousTopBidderId, toEvent(notification));
    }

    public void notifyTradeRequested(Trade trade) {
        Long sellerId = trade.getSeller().getId();
        Notification notification = notificationService.create(
                sellerId,
                NotificationType.TRADE_REQUESTED,
                "구매 신청 도착",
                "'" + trade.getProduct().getTitle() + "' 상품에 구매 신청이 도착했습니다.",
                "/trades"
        );
        notificationSseService.sendToUser(sellerId, toEvent(notification));
    }

    public void notifyAuctionWon(Auction auction, Bid winningBid) {
        Long winnerId = winningBid.getBidder().getId();
        Notification winnerNotification = notificationService.create(
                winnerId,
                NotificationType.AUCTION_WON,
                "경매 낙찰",
                "'" + auction.getProduct().getTitle() + "' 경매에 "
                        + winningBid.getAmount() + " 마일리지로 낙찰되었습니다.",
                "/auctions/" + auction.getId()
        );
        notificationSseService.sendToUser(winnerId, toEvent(winnerNotification));

        Long sellerId = auction.getSeller().getId();
        Notification sellerNotification = notificationService.create(
                sellerId,
                NotificationType.AUCTION_SOLD,
                "경매 낙찰 확정",
                "'" + auction.getProduct().getTitle() + "' 경매가 "
                        + winningBid.getBidder().getNickname() + "님에게 낙찰되었습니다.",
                "/auctions/" + auction.getId()
        );
        notificationSseService.sendToUser(sellerId, toEvent(sellerNotification));
    }

    public void notifyTradeCompleted(Trade trade) {
        String message = "'" + trade.getProduct().getTitle() + "' 거래가 완료되었습니다.";

        Notification buyerNotification = notificationService.create(
                trade.getBuyer().getId(),
                NotificationType.TRADE_COMPLETED,
                "거래 완료",
                message,
                "/trades"
        );
        notificationSseService.sendToUser(trade.getBuyer().getId(), toEvent(buyerNotification));

        Notification sellerNotification = notificationService.create(
                trade.getSeller().getId(),
                NotificationType.TRADE_COMPLETED,
                "거래 완료",
                message,
                "/trades"
        );
        notificationSseService.sendToUser(trade.getSeller().getId(), toEvent(sellerNotification));

        notifyTradeReviewRequested(trade.getBuyer().getId(), trade);
        notifyTradeReviewRequested(trade.getSeller().getId(), trade);
    }

    public void notifyTradeReviewRequested(Long recipientUserId, Trade trade) {
        Notification notification = notificationService.create(
                recipientUserId,
                NotificationType.TRADE_REVIEW_REQUESTED,
                "거래 후기 작성",
                "'" + trade.getProduct().getTitle() + "' 거래가 완료되었습니다. 상대방에게 후기를 남겨주세요.",
                "/trades"
        );
        notificationSseService.sendToUser(recipientUserId, toEvent(notification));
    }

    public void notifyReviewReceived(UserReview review, String sourceTitle) {
        Long targetUserId = review.getTargetUser().getId();
        Notification notification = notificationService.create(
                targetUserId,
                NotificationType.REVIEW_RECEIVED,
                "새 리뷰 도착",
                review.getReviewer().getNickname() + "님이 '"
                        + sourceTitle + "' 거래에 "
                        + review.getRating() + "점 리뷰를 남겼습니다.",
                "/users/" + targetUserId
        );
        notificationSseService.sendToUser(targetUserId, toEvent(notification));
    }

    public void notifyReportResolved(Report report) {
        Long reporterId = report.getReporter().getId();
        Notification notification = notificationService.create(
                reporterId,
                NotificationType.REPORT_RESOLVED,
                "신고 처리 완료",
                "신고 #" + report.getId() + " 처리가 완료되었습니다. 처리 결과: "
                        + report.getStatus().name() + " / " + report.getResolutionAction().name(),
                "/reports"
        );
        notificationSseService.sendToUser(reporterId, toEvent(notification));
    }

    public void notifyUserRestricted(Long recipientUserId, String reason, String link) {
        Notification notification = notificationService.create(
                recipientUserId,
                NotificationType.USER_RESTRICTED,
                "이용 제한 안내",
                reason == null || reason.isBlank() ? "관리자 조치로 계정 이용이 제한되었습니다." : reason,
                link
        );
        notificationSseService.sendToUser(recipientUserId, toEvent(notification));
    }

    public void notifyMileageWithdrawalRequested(MileageWithdrawal withdrawal) {
        for (User admin : userRepository.findAllByRoleIn(List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN))) {
            Notification notification = notificationService.create(
                    admin.getId(),
                    NotificationType.MILEAGE_WITHDRAWAL_REQUESTED,
                    "출금 요청 접수",
                    withdrawal.getUser().getNickname() + "님이 "
                            + withdrawal.getAmount() + "원 출금을 요청했습니다.",
                    "/admin"
            );
            notificationSseService.sendToUser(admin.getId(), toEvent(notification));
        }
    }

    public void notifyChatMessage(ChatRoom room, ChatMessage message) {
        Long senderId = message.getSender().getId();
        Long receiverId = room.getParticipantA().getId().equals(senderId)
                ? room.getParticipantB().getId()
                : room.getParticipantA().getId();

        Notification notification = notificationService.create(
                receiverId,
                NotificationType.CHAT_MESSAGE,
                "새 채팅 메시지",
                message.getSender().getNickname() + ": " + message.getContent(),
                "/chat/rooms/" + room.getId()
        );
        notificationSseService.sendToUser(receiverId, toEvent(notification));
    }

    private NotificationEventResponse toEvent(Notification notification) {
        return new NotificationEventResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.getOccurredAt(),
                notification.getReadAt()
        );
    }
}
