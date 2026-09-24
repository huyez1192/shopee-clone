package com.ecommerce.minishopee.statistics.projection;

import java.math.BigDecimal;

public interface TopProductProjection {
    Long getProductId();
    String getProductName();
    String getImageUrl();
    Long getUnitsSold();
    BigDecimal getMerchandiseValue();
}
