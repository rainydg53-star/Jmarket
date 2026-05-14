package com.jmarket.payment.controller;

import com.jmarket.payment.dto.PaymentApproveRequest;
import com.jmarket.payment.dto.PaymentReadyRequest;
import com.jmarket.payment.dto.PaymentReadyResponse;
import com.jmarket.payment.dto.PaymentResponse;
import com.jmarket.payment.dto.PaymentStatusUpdateRequest;
import com.jmarket.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/kakaopay")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/ready")
    public PaymentReadyResponse ready(
            @Valid @RequestBody PaymentReadyRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return paymentService.requestKakaoPay(request, email);
    }

    @PostMapping("/approve")
    public PaymentResponse approve(
            @Valid @RequestBody PaymentApproveRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return paymentService.approveKakaoPay(request, email);
    }

    @PostMapping("/cancel")
    public PaymentResponse cancel(
            @Valid @RequestBody PaymentStatusUpdateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return paymentService.markCanceled(request, email);
    }

    @PostMapping("/fail")
    public PaymentResponse fail(
            @Valid @RequestBody PaymentStatusUpdateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return paymentService.markFailed(request, email);
    }

    @GetMapping("/me")
    public List<PaymentResponse> myPayments(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return paymentService.getMyPayments(email);
    }
}
