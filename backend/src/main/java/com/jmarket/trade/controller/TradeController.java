package com.jmarket.trade.controller;

import com.jmarket.trade.dto.TradeCreateRequest;
import com.jmarket.trade.dto.TradeListRole;
import com.jmarket.trade.dto.TradeResponse;
import com.jmarket.trade.service.TradeService;
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
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping
    public TradeResponse create(
            @Valid @RequestBody TradeCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return tradeService.create(request, email);
    }

    @GetMapping("/{tradeId}")
    public TradeResponse getById(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return tradeService.getById(tradeId, email);
    }

    @GetMapping("/me")
    public List<TradeResponse> getMyTrades(
            @AuthenticationPrincipal(expression = "username") String email,
            @RequestParam(defaultValue = "ALL") TradeListRole role
    ) {
        return tradeService.getMyTrades(role, email);
    }

    @PatchMapping("/{tradeId}/accept")
    public TradeResponse accept(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return tradeService.accept(tradeId, email);
    }

    @PatchMapping("/{tradeId}/complete")
    public TradeResponse complete(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return tradeService.complete(tradeId, email);
    }

    @PatchMapping("/{tradeId}/cancel")
    public TradeResponse cancel(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return tradeService.cancel(tradeId, email);
    }
}
