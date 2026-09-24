package com.ecommerce.minishopee.statistics.dto;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId,
        String productName,
        String imageUrl,
        long unitsSold,
        BigDecimal merchandiseValue
) {
}
