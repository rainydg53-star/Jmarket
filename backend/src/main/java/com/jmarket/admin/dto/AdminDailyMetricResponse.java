package com.jmarket.admin.dto;

import java.time.LocalDate;

public record AdminDailyMetricResponse(
        LocalDate date,
        long activeUsers,
        long completedTrades,
        long processedReports,
        long mileageCharged,
        long mileageUsed
) {
}
