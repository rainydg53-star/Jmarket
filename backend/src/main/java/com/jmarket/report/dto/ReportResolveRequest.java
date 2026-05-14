package com.jmarket.report.dto;

import com.jmarket.report.domain.ReportResolutionAction;
import com.jmarket.report.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportResolveRequest(
        @NotNull ReportStatus status,
        @NotNull ReportResolutionAction resolutionAction,
        @Size(max = 1000) String resolutionMemo
) {
}
