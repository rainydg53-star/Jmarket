package com.jmarket.report.controller;

import com.jmarket.report.dto.ReportResolveRequest;
import com.jmarket.report.dto.ReportResponse;
import com.jmarket.report.service.ReportService;
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
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportResponse> getAll(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return reportService.getAllForAdmin(email);
    }

    @GetMapping("/{reportId}")
    public ReportResponse getById(
            @PathVariable Long reportId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return reportService.getById(reportId, email);
    }

    @PatchMapping("/{reportId}/resolve")
    public ReportResponse resolve(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportResolveRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return reportService.resolveByAdmin(reportId, request, email);
    }
}
