package com.ecommerce.minishopee.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminStatisticsOverviewResponse(
        LocalDate from,
        LocalDate to,
        long lifetimeTotalUsers,
        long lifetimeTotalShops,
        long lifetimeTotalProducts,
        long lifetimeTotalOrders,
        long completedOrders,
        BigDecimal completedGmv,
        BigDecimal completedMerchandiseValue,
        long unitsSold,
        List<OrderStatusCountResponse> orderStatusDistribution
) {
}
