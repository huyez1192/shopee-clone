package com.ecommerce.minishopee.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SellerStatisticsOverviewResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal completedRevenue,
        BigDecimal completedGmv,
        long totalOrdersCreated,
        long completedOrderCount,
        long unitsSold,
        List<OrderStatusCountResponse> orderStatusDistribution,
        RatingSummaryResponse ratingSummary
) {
}
