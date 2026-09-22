package com.ecommerce.minishopee.review.service;

import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.review.dto.request.SellerReplyRequest;
import com.ecommerce.minishopee.review.dto.response.ReviewResponse;
import com.ecommerce.minishopee.review.entity.Review;
import com.ecommerce.minishopee.review.exception.ReviewAccessDeniedException;
import com.ecommerce.minishopee.review.exception.ReviewConflictException;
import com.ecommerce.minishopee.review.exception.ReviewNotFoundException;
import com.ecommerce.minishopee.review.mapper.ReviewMapper;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.exception.ShopNotFoundException;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SellerReviewService {
    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;

    public SellerReviewService(ReviewRepository reviewRepository, ShopRepository shopRepository) {
        this.reviewRepository = reviewRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(
            AppUser seller, int page, int size, Boolean replied, Integer rating
    ) {
        ReviewService.validatePage(page, size);
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BadRequestException("rating phải từ 1 đến 5.");
        }
        Shop shop = getSellerShop(seller);
        Page<ReviewResponse> result = reviewRepository.findDetailedByShopId(
                        shop.getId(), replied, rating,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                )
                .map(ReviewMapper::toResponse);
        return ReviewService.toPageResponse(result);
    }

    @Transactional
    public ReviewResponse reply(Long reviewId, AppUser seller, SellerReplyRequest request) {
        Shop shop = getSellerShop(seller);
        Review review = reviewRepository.findDetailedByIdForUpdate(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getProduct().getShop().getId().equals(shop.getId())) {
            throw new ReviewAccessDeniedException("Bạn không thể phản hồi đánh giá của shop khác.");
        }
        if (review.getSellerReply() != null) {
            throw new ReviewConflictException("Đánh giá này đã được shop phản hồi.");
        }

        review.setSellerReply(request.getReply().trim());
        review.setRepliedAt(LocalDateTime.now());
        return ReviewMapper.toResponse(reviewRepository.save(review));
    }

    private Shop getSellerShop(AppUser seller) {
        return shopRepository.findByOwnerId(seller.getId())
                .orElseThrow(() -> new ShopNotFoundException("Không tìm thấy shop của người bán."));
    }
}
