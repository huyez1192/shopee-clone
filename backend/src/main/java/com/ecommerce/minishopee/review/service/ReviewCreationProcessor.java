package com.ecommerce.minishopee.review.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.order.repository.OrderItemRepository;
import com.ecommerce.minishopee.review.dto.request.CreateReviewRequest;
import com.ecommerce.minishopee.review.dto.response.ReviewResponse;
import com.ecommerce.minishopee.review.entity.Review;
import com.ecommerce.minishopee.review.exception.OrderItemNotFoundException;
import com.ecommerce.minishopee.review.exception.ReviewAccessDeniedException;
import com.ecommerce.minishopee.review.exception.ReviewConflictException;
import com.ecommerce.minishopee.review.mapper.ReviewMapper;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewCreationProcessor {
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    public ReviewCreationProcessor(OrderItemRepository orderItemRepository, ReviewRepository reviewRepository) {
        this.orderItemRepository = orderItemRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public ReviewResponse create(AppUser currentUser, CreateReviewRequest request) {
        OrderItem orderItem = orderItemRepository.findDetailedById(request.getOrderItemId())
                .orElseThrow(() -> new OrderItemNotFoundException(request.getOrderItemId()));

        if (!orderItem.getOrder().getBuyer().getId().equals(currentUser.getId())) {
            throw new ReviewAccessDeniedException("Bạn không thể đánh giá sản phẩm thuộc đơn hàng của người khác.");
        }
        if (orderItem.getOrder().getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể đánh giá sản phẩm trong đơn hàng đã hoàn thành.");
        }
        if (reviewRepository.existsByOrderItem_Id(orderItem.getId())) {
            throw new ReviewConflictException("Sản phẩm trong đơn hàng này đã được đánh giá.");
        }

        Review review = new Review();
        review.setBuyer(currentUser);
        review.setProduct(orderItem.getProduct());
        review.setOrderItem(orderItem);
        review.setRating(request.getRating());
        review.setComment(normalize(request.getComment()));

        return ReviewMapper.toResponse(reviewRepository.saveAndFlush(review));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
