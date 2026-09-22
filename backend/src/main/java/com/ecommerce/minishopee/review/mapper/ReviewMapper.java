package com.ecommerce.minishopee.review.mapper;

import com.ecommerce.minishopee.review.dto.response.ReviewResponse;
import com.ecommerce.minishopee.review.entity.Review;

public final class ReviewMapper {
    private ReviewMapper() {
    }

    public static ReviewResponse toResponse(Review review) {
        String displayName = review.getBuyer().getFullName();
        if (displayName == null || displayName.isBlank()) {
            displayName = "Người mua";
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .orderItemId(review.getOrderItem().getId())
                .productName(review.getOrderItem().getProductNameSnapshot())
                .productImageUrl(review.getOrderItem().getProductImageUrlSnapshot())
                .buyerDisplayName(displayName)
                .buyerAvatarUrl(review.getBuyer().getAvatarUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .sellerReply(review.getSellerReply())
                .repliedAt(review.getRepliedAt())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
