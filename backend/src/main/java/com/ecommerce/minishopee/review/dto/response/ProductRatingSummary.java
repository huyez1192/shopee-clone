package com.ecommerce.minishopee.review.dto.response;

public record ProductRatingSummary(double averageRating, long reviewCount) {
    public static final ProductRatingSummary EMPTY = new ProductRatingSummary(0.0, 0L);
}
