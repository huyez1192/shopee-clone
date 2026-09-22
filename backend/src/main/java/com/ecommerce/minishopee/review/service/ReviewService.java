package com.ecommerce.minishopee.review.service;

import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.product.exception.ProductNotFoundException;
import com.ecommerce.minishopee.product.repository.ProductRepository;
import com.ecommerce.minishopee.review.dto.request.CreateReviewRequest;
import com.ecommerce.minishopee.review.dto.response.ReviewResponse;
import com.ecommerce.minishopee.review.exception.ReviewConflictException;
import com.ecommerce.minishopee.review.mapper.ReviewMapper;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewCreationProcessor creationProcessor;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(
            ReviewCreationProcessor creationProcessor,
            ReviewRepository reviewRepository,
            ProductRepository productRepository
    ) {
        this.creationProcessor = creationProcessor;
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    public ReviewResponse createReview(AppUser currentUser, CreateReviewRequest request) {
        try {
            return creationProcessor.create(currentUser, request);
        } catch (DataIntegrityViolationException exception) {
            // The processor transaction has already rolled back before this verification runs.
            if (isOrderItemUniqueViolation(exception)
                    && reviewRepository.findByOrderItem_Id(request.getOrderItemId()).isPresent()) {
                throw new ReviewConflictException("Sản phẩm trong đơn hàng này đã được đánh giá.");
            }
            throw exception;
        }
    }

    private boolean isOrderItemUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null
                    && cause.getMessage().toLowerCase().contains("uk_reviews_order_item_id")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(Long productId, int page, int size) {
        validatePage(page, size);
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        Page<ReviewResponse> result = reviewRepository.findDetailedByProductId(
                        productId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                )
                .map(ReviewMapper::toResponse);
        return toPageResponse(result);
    }

    static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("page phải >= 0 và size phải từ 1 đến 100.");
        }
    }
}
