package com.ecommerce.minishopee.statistics.projection;

import java.math.BigDecimal;

public interface OrderPeriodProjection {
    Integer getPeriodYear();
    Integer getPeriodMonth();
    Integer getPeriodDay();
    BigDecimal getCompletedRevenue();
    BigDecimal getCompletedGmv();
    Long getCompletedOrderCount();
}
