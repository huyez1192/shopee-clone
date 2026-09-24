package com.ecommerce.minishopee.statistics.dto;

public record RatingSummaryResponse(
        double averageRating,
        long reviewCount,
        long fiveStarCount,
        long fourStarCount,
        long threeStarCount,
        long twoStarCount,
        long oneStarCount
) {
}
