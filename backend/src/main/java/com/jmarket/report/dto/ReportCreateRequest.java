package com.jmarket.report.dto;

import com.jmarket.report.domain.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull ReportTargetType targetType,
        @NotNull Long targetId,
        @NotBlank @Size(max = 100) String reason,
        @NotBlank @Size(max = 5000) String detail
) {
}
