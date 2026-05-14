package com.jmarket.support.controller;

import com.jmarket.support.dto.SupportInquiryCreateRequest;
import com.jmarket.support.dto.SupportInquiryDetailResponse;
import com.jmarket.support.dto.SupportInquiryStatusUpdateRequest;
import com.jmarket.support.dto.SupportInquirySummaryResponse;
import com.jmarket.support.service.SupportInquiryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/inquiries")
public class SupportInquiryController {

    private final SupportInquiryService supportInquiryService;

    public SupportInquiryController(SupportInquiryService supportInquiryService) {
        this.supportInquiryService = supportInquiryService;
    }

    @PostMapping
    public SupportInquiryDetailResponse create(
            @Valid @RequestBody SupportInquiryCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return supportInquiryService.create(request, email);
    }

    @GetMapping("/me")
    public List<SupportInquirySummaryResponse> getMyInquiries(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return supportInquiryService.getMyInquiries(email);
    }

    @GetMapping("/{inquiryId}")
    public SupportInquiryDetailResponse getById(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return supportInquiryService.getById(inquiryId, email);
    }

    @PatchMapping("/{inquiryId}/status")
    public SupportInquiryDetailResponse updateMyStatus(
            @PathVariable Long inquiryId,
            @Valid @RequestBody SupportInquiryStatusUpdateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return supportInquiryService.updateStatusByMember(inquiryId, request, email);
    }
}
