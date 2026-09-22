package com.ecommerce.minishopee.review.repository;

public interface ProductRatingProjection {
    Long getProductId();
    Double getAverageRating();
    Long getReviewCount();
}
