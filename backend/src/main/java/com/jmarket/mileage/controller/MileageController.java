package com.jmarket.mileage.controller;

import com.jmarket.mileage.dto.MileageAccountResponse;
import com.jmarket.mileage.dto.MileageChargeRequest;
import com.jmarket.mileage.dto.MileageLedgerResponse;
import com.jmarket.mileage.dto.MileageUseRequest;
import com.jmarket.mileage.dto.MileageWithdrawalCreateRequest;
import com.jmarket.mileage.dto.MileageWithdrawalResponse;
import com.jmarket.mileage.service.MileageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mileage")
public class MileageController {

    private final MileageService mileageService;

    public MileageController(MileageService mileageService) {
        this.mileageService = mileageService;
    }

    @GetMapping("/me")
    public MileageAccountResponse getMyAccount(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return mileageService.getMyAccount(email);
    }

    @PostMapping("/charge")
    @PreAuthorize("hasRole('ADMIN')")
    public MileageAccountResponse charge(
            @Valid @RequestBody MileageChargeRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return mileageService.chargeMyMileage(email, request.amount());
    }

    @PostMapping("/use")
    @PreAuthorize("hasRole('ADMIN')")
    public MileageAccountResponse use(
            @Valid @RequestBody MileageUseRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return mileageService.useMyMileage(email, request.amount());
    }

    @GetMapping("/me/ledger")
    public List<MileageLedgerResponse> getMyLedger(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return mileageService.getMyLedger(email);
    }

    @PostMapping("/withdrawals")
    public MileageWithdrawalResponse requestWithdrawal(
            @Valid @RequestBody MileageWithdrawalCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return mileageService.requestWithdrawal(email, request);
    }

    @GetMapping("/me/withdrawals")
    public List<MileageWithdrawalResponse> getMyWithdrawals(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return mileageService.getMyWithdrawals(email);
    }
}
