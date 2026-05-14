package com.jmarket.report.controller;

import com.jmarket.report.dto.ReportCreateRequest;
import com.jmarket.report.dto.ReportResponse;
import com.jmarket.report.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ReportResponse create(
            @Valid @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return reportService.create(request, email);
    }

    @GetMapping("/me")
    public List<ReportResponse> getMyReports(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return reportService.getMyReports(email);
    }

    @GetMapping("/{reportId}")
    public ReportResponse getById(
            @PathVariable Long reportId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return reportService.getById(reportId, email);
    }
}
