package com.jmarket.admin.dto;

import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long todayJoinedUsers,
        long directProducts,
        long auctionProducts,
        long openAuctions,
        long completedTrades,
        long totalMileageCharged,
        long totalMileageUsed,
        long approvedPaymentAmount,
        List<AdminDailyMetricResponse> dailyMetrics,
        List<AdminAuditDailyMetricResponse> auditDailyMetrics,
        List<AdminAuditActionMetricResponse> auditActionMetrics
) {
}
