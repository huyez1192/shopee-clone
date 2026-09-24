package com.ecommerce.minishopee.statistics.service;

import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.review.repository.ProductRatingSummaryProjection;
import com.ecommerce.minishopee.statistics.dto.OrderStatusCountResponse;
import com.ecommerce.minishopee.statistics.dto.RatingSummaryResponse;
import com.ecommerce.minishopee.statistics.projection.OrderStatusCountProjection;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

final class StatisticsMapper {
    private StatisticsMapper() {
    }

    static List<OrderStatusCountResponse> statusDistribution(List<OrderStatusCountProjection> rows) {
        EnumMap<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        rows.forEach(row -> counts.put(row.getStatus(), row.getOrderCount()));
        return Arrays.stream(OrderStatus.values())
                .map(status -> new OrderStatusCountResponse(status, counts.getOrDefault(status, 0L)))
                .toList();
    }

    static RatingSummaryResponse rating(ProductRatingSummaryProjection row) {
        return new RatingSummaryResponse(
                row.getAverageRating() == null ? 0.0 : row.getAverageRating(),
                value(row.getReviewCount()), value(row.getFiveStarCount()),
                value(row.getFourStarCount()), value(row.getThreeStarCount()),
                value(row.getTwoStarCount()), value(row.getOneStarCount())
        );
    }

    private static long value(Long value) {
        return value == null ? 0L : value;
    }
}
