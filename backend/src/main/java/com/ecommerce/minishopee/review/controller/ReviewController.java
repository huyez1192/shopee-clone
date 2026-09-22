package com.ecommerce.minishopee.review.controller;

import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.review.dto.request.CreateReviewRequest;
import com.ecommerce.minishopee.review.dto.response.ReviewResponse;
import com.ecommerce.minishopee.review.service.ReviewService;
import com.ecommerce.minishopee.user.entity.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return reviewService.createReview(currentUser, request);
    }

    @GetMapping("/api/products/{productId}/reviews")
    public PageResponse<ReviewResponse> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reviewService.getProductReviews(productId, page, size);
    }
}
