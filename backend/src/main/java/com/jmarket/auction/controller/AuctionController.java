package com.jmarket.auction.controller;

import com.jmarket.auction.dto.AuctionCreateRequest;
import com.jmarket.auction.dto.AuctionResponse;
import com.jmarket.auction.dto.BidRequest;
import com.jmarket.auction.dto.BidResponse;
import com.jmarket.auction.service.AuctionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    public AuctionResponse create(
            @Valid @RequestBody AuctionCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return auctionService.create(request, email);
    }

    @GetMapping
    public List<AuctionResponse> getOpenAuctions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "ENDING_SOON") String sort
    ) {
        return auctionService.getOpenAuctions(keyword, category, sort);
    }

    @GetMapping("/me/purchases")
    public List<AuctionResponse> getMyWonAuctions(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return auctionService.getMyWonAuctions(email);
    }

    @GetMapping("/{auctionId}")
    public AuctionResponse getById(@PathVariable Long auctionId) {
        return auctionService.getById(auctionId);
    }

    @PostMapping("/{auctionId}/bids")
    public BidResponse placeBid(
            @PathVariable Long auctionId,
            @Valid @RequestBody BidRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return auctionService.placeBid(auctionId, request, email);
    }

    @GetMapping("/{auctionId}/bids")
    public List<BidResponse> getBids(@PathVariable Long auctionId) {
        return auctionService.getBids(auctionId);
    }

    @PatchMapping("/{auctionId}/close")
    public AuctionResponse close(
            @PathVariable Long auctionId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return auctionService.close(auctionId, email);
    }
}
