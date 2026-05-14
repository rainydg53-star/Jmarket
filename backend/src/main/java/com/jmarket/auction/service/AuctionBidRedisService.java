package com.jmarket.auction.service;

import com.jmarket.auction.domain.Auction;
import com.jmarket.auction.domain.Bid;
import com.jmarket.auction.dto.AuctionBidSnapshot;
import com.jmarket.auction.dto.RedisBidResult;
import com.jmarket.auction.repository.BidRepository;
import com.jmarket.auth.domain.User;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class AuctionBidRedisService {

    private static final Logger log = LoggerFactory.getLogger(AuctionBidRedisService.class);

    private final StringRedisTemplate redisTemplate;
    private final BidRepository bidRepository;
    private final DefaultRedisScript<List> placeBidScript;

    public AuctionBidRedisService(StringRedisTemplate redisTemplate, BidRepository bidRepository) {
        this.redisTemplate = redisTemplate;
        this.bidRepository = bidRepository;
        this.placeBidScript = new DefaultRedisScript<>();
        this.placeBidScript.setResultType(List.class);
        this.placeBidScript.setScriptText("""
                local current = tonumber(redis.call('GET', KEYS[1]) or ARGV[1])
                local currentBidder = redis.call('GET', KEYS[2])
                local currentNickname = redis.call('GET', KEYS[3]) or ''
                local currentCount = tonumber(redis.call('GET', KEYS[4]) or '0')
                local bidderId = ARGV[2]
                local bidderNickname = ARGV[3]
                local requestedAmount = tonumber(ARGV[4])
                local instantBuyPrice = tonumber(ARGV[5])

                if currentBidder ~= false and currentBidder == bidderId then
                    return {-2, current, currentBidder, currentNickname, currentCount, 0, 0}
                end

                local instantTriggered = instantBuyPrice > 0 and requestedAmount >= instantBuyPrice
                local effectiveAmount = requestedAmount
                if instantTriggered then
                    effectiveAmount = instantBuyPrice
                end

                local minimumAllowed = math.ceil(current * 1.1)
                if (not instantTriggered) and effectiveAmount < minimumAllowed then
                    return {-1, current, currentBidder or '', currentNickname, currentCount, 0, minimumAllowed}
                end

                redis.call('SET', KEYS[1], tostring(effectiveAmount))
                redis.call('SET', KEYS[2], bidderId)
                redis.call('SET', KEYS[3], bidderNickname)
                local nextCount = redis.call('INCR', KEYS[4])
                return {1, current, currentBidder or '', currentNickname, nextCount, effectiveAmount, minimumAllowed}
                """);
    }

    public AuctionBidSnapshot getSnapshot(Auction auction) {
        try {
            String amount = redisTemplate.opsForValue().get(amountKey(auction.getId()));
            if (amount == null) {
                initializeFromDatabase(auction);
            }

            Long currentHighestBid = parseLong(redisTemplate.opsForValue().get(amountKey(auction.getId())), auction.getStartPrice());
            Long currentHighestBidderId = parseNullableLong(redisTemplate.opsForValue().get(bidderKey(auction.getId())));
            String currentHighestBidderNickname = redisTemplate.opsForValue().get(nicknameKey(auction.getId()));
            Long totalBidCount = parseLong(redisTemplate.opsForValue().get(countKey(auction.getId())), 0L);
            return new AuctionBidSnapshot(currentHighestBid, currentHighestBidderId, currentHighestBidderNickname, totalBidCount);
        } catch (RuntimeException ex) {
            log.warn("Redis auction bid snapshot failed. auctionId={}", auction.getId(), ex);
            return getDatabaseSnapshot(auction);
        }
    }

    public RedisBidResult placeBid(Auction auction, User bidder, Long requestedAmount, Long instantBuyPrice) {
        try {
            initializeFromDatabase(auction);
            List<?> result = redisTemplate.execute(
                    placeBidScript,
                    List.of(amountKey(auction.getId()), bidderKey(auction.getId()), nicknameKey(auction.getId()), countKey(auction.getId())),
                    String.valueOf(auction.getStartPrice()),
                    String.valueOf(bidder.getId()),
                    bidder.getNickname(),
                    String.valueOf(requestedAmount),
                    String.valueOf(instantBuyPrice == null ? 0L : instantBuyPrice)
            );

            if (result == null || result.isEmpty()) {
                return RedisBidResult.rejectedTooLow(auction.getStartPrice(), auction.getStartPrice());
            }

            long code = asLong(result.get(0), 0L);
            Long previousAmount = asLong(result.get(1), auction.getStartPrice());
            Long previousBidderId = parseNullableLong(String.valueOf(result.get(2)));
            String previousNickname = String.valueOf(result.get(3));
            Long totalBidCount = asLong(result.get(4), 0L);
            Long effectiveAmount = asLong(result.get(5), 0L);
            Long minimumAllowed = asLong(result.get(6), 0L);

            if (code == -2L) {
                return RedisBidResult.rejectedTopBidder(previousAmount, previousBidderId);
            }
            if (code == -1L) {
                return RedisBidResult.rejectedTooLow(previousAmount, minimumAllowed);
            }
            return new RedisBidResult(true, false, previousAmount, previousBidderId, previousNickname, totalBidCount, effectiveAmount, minimumAllowed);
        } catch (RuntimeException ex) {
            log.warn("Redis auction bid update failed. falling back to database snapshot. auctionId={}", auction.getId(), ex);
            AuctionBidSnapshot snapshot = getDatabaseSnapshot(auction);
            if (snapshot.currentHighestBidderId() != null && snapshot.currentHighestBidderId().equals(bidder.getId())) {
                return RedisBidResult.rejectedTopBidder(snapshot.currentHighestBid(), snapshot.currentHighestBidderId());
            }
            long minimumAllowed = (long) Math.ceil(snapshot.currentHighestBid() * 1.1d);
            boolean instantTriggered = instantBuyPrice != null && requestedAmount >= instantBuyPrice;
            long effectiveAmount = instantTriggered ? instantBuyPrice : requestedAmount;
            if (!instantTriggered && effectiveAmount < minimumAllowed) {
                return RedisBidResult.rejectedTooLow(snapshot.currentHighestBid(), minimumAllowed);
            }
            return new RedisBidResult(
                    true,
                    false,
                    snapshot.currentHighestBid(),
                    snapshot.currentHighestBidderId(),
                    snapshot.currentHighestBidderNickname(),
                    snapshot.totalBidCount() + 1,
                    effectiveAmount,
                    minimumAllowed
            );
        }
    }

    public void initialize(Auction auction) {
        try {
            redisTemplate.opsForValue().set(amountKey(auction.getId()), String.valueOf(auction.getStartPrice()));
            redisTemplate.delete(bidderKey(auction.getId()));
            redisTemplate.delete(nicknameKey(auction.getId()));
            redisTemplate.opsForValue().set(countKey(auction.getId()), "0");
        } catch (RuntimeException ex) {
            log.warn("Redis auction bid initialization failed. auctionId={}", auction.getId(), ex);
        }
    }

    private void initializeFromDatabase(Auction auction) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(amountKey(auction.getId())))) {
            return;
        }
        Bid topBid = bidRepository.findTopByAuctionIdOrderByAmountDescBidAtAsc(auction.getId()).orElse(null);
        long count = bidRepository.countByAuctionId(auction.getId());
        redisTemplate.opsForValue().set(amountKey(auction.getId()), String.valueOf(topBid != null ? topBid.getAmount() : auction.getStartPrice()));
        redisTemplate.opsForValue().set(countKey(auction.getId()), String.valueOf(count));
        if (topBid != null) {
            redisTemplate.opsForValue().set(bidderKey(auction.getId()), String.valueOf(topBid.getBidder().getId()));
            redisTemplate.opsForValue().set(nicknameKey(auction.getId()), topBid.getBidder().getNickname());
        }
    }

    private AuctionBidSnapshot getDatabaseSnapshot(Auction auction) {
        Bid topBid = bidRepository.findTopByAuctionIdOrderByAmountDescBidAtAsc(auction.getId()).orElse(null);
        return new AuctionBidSnapshot(
                topBid != null ? topBid.getAmount() : auction.getStartPrice(),
                topBid != null ? topBid.getBidder().getId() : null,
                topBid != null ? topBid.getBidder().getNickname() : null,
                bidRepository.countByAuctionId(auction.getId())
        );
    }

    private String amountKey(Long auctionId) {
        return "jmarket:auction:" + auctionId + ":bid:amount";
    }

    private String bidderKey(Long auctionId) {
        return "jmarket:auction:" + auctionId + ":bid:bidder-id";
    }

    private String nicknameKey(Long auctionId) {
        return "jmarket:auction:" + auctionId + ":bid:bidder-nickname";
    }

    private String countKey(Long auctionId) {
        return "jmarket:auction:" + auctionId + ":bid:count";
    }

    private Long parseLong(String value, Long defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Long parseNullableLong(String value) {
        try {
            return value == null || value.isBlank() || "false".equals(value) ? null : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long asLong(Object value, Long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return parseLong(String.valueOf(value), defaultValue);
    }
}
