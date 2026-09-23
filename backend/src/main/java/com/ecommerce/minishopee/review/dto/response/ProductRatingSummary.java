package com.ecommerce.minishopee.review.dto.response;

public record ProductRatingSummary(
        double averageRating,
        long reviewCount,
        long fiveStarCount,
        long fourStarCount,
        long threeStarCount,
        long twoStarCount,
        long oneStarCount
) {
    public static final ProductRatingSummary EMPTY = new ProductRatingSummary(0.0, 0L, 0L, 0L, 0L, 0L, 0L);
}
