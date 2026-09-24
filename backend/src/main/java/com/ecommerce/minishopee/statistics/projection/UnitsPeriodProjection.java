package com.ecommerce.minishopee.statistics.projection;

public interface UnitsPeriodProjection {
    Integer getPeriodYear();
    Integer getPeriodMonth();
    Integer getPeriodDay();
    Long getUnitsSold();
}
