package com.ecommerce.minishopee.statistics.dto;

import java.math.BigDecimal;

public record RevenueSeriesItemResponse(
        String period,
        BigDecimal completedRevenue,
        BigDecimal completedGmv,
        long completedOrderCount,
        long unitsSold
) {
}
