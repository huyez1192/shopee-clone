package com.ecommerce.minishopee.review.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponse {
    private Long id;
    private Long productId;
    private Long orderItemId;
    private String productName;
    private String productImageUrl;
    private String buyerDisplayName;
    private String buyerAvatarUrl;
    private Integer rating;
    private String comment;
    private String sellerReply;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
}
