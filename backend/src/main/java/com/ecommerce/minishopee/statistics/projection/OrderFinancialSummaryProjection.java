package com.ecommerce.minishopee.statistics.projection;

import java.math.BigDecimal;

public interface OrderFinancialSummaryProjection {
    BigDecimal getCompletedRevenue();
    BigDecimal getCompletedGmv();
    Long getCompletedOrderCount();
}
