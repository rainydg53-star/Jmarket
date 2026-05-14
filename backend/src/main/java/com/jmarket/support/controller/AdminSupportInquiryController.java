package com.jmarket.support.controller;

import com.jmarket.support.dto.SupportInquiryAnswerRequest;
import com.jmarket.support.dto.SupportInquiryDetailResponse;
import com.jmarket.support.dto.SupportInquiryStatusUpdateRequest;
import com.jmarket.support.dto.SupportInquirySummaryResponse;
import com.jmarket.support.service.SupportInquiryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/support/inquiries")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportInquiryController {

    private final SupportInquiryService supportInquiryService;

    public AdminSupportInquiryController(SupportInquiryService supportInquiryService) {
        this.supportInquiryService = supportInquiryService;
    }

    @GetMapping
    public List<SupportInquirySummaryResponse> getAll(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return supportInquiryService.getAllForAdmin(email);
    }

    @GetMapping("/{inquiryId}")
    public SupportInquiryDetailResponse getById(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return supportInquiryService.getById(inquiryId, email);
    }

    @PatchMapping("/{inquiryId}/answer")
    public SupportInquiryDetailResponse answer(
            @PathVariable Long inquiryId,
            @Valid @RequestBody SupportInquiryAnswerRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return supportInquiryService.answerByAdmin(inquiryId, request, email);
    }

    @PatchMapping("/{inquiryId}/status")
    public SupportInquiryDetailResponse updateStatus(
            @PathVariable Long inquiryId,
            @Valid @RequestBody SupportInquiryStatusUpdateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return supportInquiryService.updateStatusByAdmin(inquiryId, request, email);
    }
}
