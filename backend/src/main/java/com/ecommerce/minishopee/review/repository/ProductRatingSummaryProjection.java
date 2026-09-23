package com.ecommerce.minishopee.review.repository;

public interface ProductRatingSummaryProjection {
    Double getAverageRating();
    Long getReviewCount();
    Long getFiveStarCount();
    Long getFourStarCount();
    Long getThreeStarCount();
    Long getTwoStarCount();
    Long getOneStarCount();
}
