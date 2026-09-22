package com.ecommerce.minishopee.review.controller;

import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.review.dto.request.SellerReplyRequest;
import com.ecommerce.minishopee.review.dto.response.ReviewResponse;
import com.ecommerce.minishopee.review.service.SellerReviewService;
import com.ecommerce.minishopee.user.entity.AppUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/reviews")
public class SellerReviewController {
    private final SellerReviewService sellerReviewService;

    public SellerReviewController(SellerReviewService sellerReviewService) {
        this.sellerReviewService = sellerReviewService;
    }

    @GetMapping
    public PageResponse<ReviewResponse> getReviews(
            @AuthenticationPrincipal AppUser seller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean replied,
            @RequestParam(required = false) Integer rating
    ) {
        return sellerReviewService.getReviews(seller, page, size, replied, rating);
    }

    @PatchMapping("/{reviewId}/reply")
    public ReviewResponse reply(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal AppUser seller,
            @Valid @RequestBody SellerReplyRequest request
    ) {
        return sellerReviewService.reply(reviewId, seller, request);
    }
}
