package com.jmarket.report.dto;

import com.jmarket.report.domain.Report;
import com.jmarket.report.domain.ReportResolutionAction;
import com.jmarket.report.domain.ReportStatus;
import com.jmarket.report.domain.ReportTargetType;
import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long reporterId,
        String reporterEmail,
        String reporterNickname,
        ReportTargetType targetType,
        Long targetId,
        String targetSummary,
        Long targetOwnerUserId,
        String targetOwnerNickname,
        long targetReportCount,
        long targetOwnerReportCount,
        String reason,
        String detail,
        ReportStatus status,
        ReportResolutionAction resolutionAction,
        String resolutionMemo,
        Long processedById,
        String processedByEmail,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
    public static ReportResponse from(Report report) {
        return from(report, null, null, null, 0L, 0L);
    }

    public static ReportResponse from(
            Report report,
            String targetSummary,
            Long targetOwnerUserId,
            String targetOwnerNickname,
            long targetReportCount,
            long targetOwnerReportCount
    ) {
        return new ReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getEmail(),
                report.getReporter().getNickname(),
                report.getTargetType(),
                report.getTargetId(),
                targetSummary,
                targetOwnerUserId,
                targetOwnerNickname,
                targetReportCount,
                targetOwnerReportCount,
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getResolutionAction(),
                report.getResolutionMemo(),
                report.getProcessedBy() != null ? report.getProcessedBy().getId() : null,
                report.getProcessedBy() != null ? report.getProcessedBy().getEmail() : null,
                report.getProcessedAt(),
                report.getCreatedAt()
        );
    }
}
