package com.jmarket.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionAutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionAutoCloseScheduler.class);

    private final AuctionService auctionService;

    public AuctionAutoCloseScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Scheduled(fixedDelayString = "${auction.auto-close-interval-ms:30000}")
    public void autoCloseExpiredAuctions() {
        int closedCount = auctionService.closeExpiredOpenAuctions();
        if (closedCount > 0) {
            log.info("Auto-closed {} auction(s)", closedCount);
        }
    }
}
