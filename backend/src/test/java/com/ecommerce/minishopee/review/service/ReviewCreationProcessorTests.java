package com.ecommerce.minishopee.review.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.order.repository.OrderItemRepository;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.review.dto.request.CreateReviewRequest;
import com.ecommerce.minishopee.review.entity.Review;
import com.ecommerce.minishopee.review.exception.OrderItemNotFoundException;
import com.ecommerce.minishopee.review.exception.ReviewAccessDeniedException;
import com.ecommerce.minishopee.review.exception.ReviewConflictException;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewCreationProcessorTests {
    @Mock OrderItemRepository orderItemRepository;
    @Mock ReviewRepository reviewRepository;

    ReviewCreationProcessor processor;
    AppUser buyer;
    OrderItem item;
    CreateReviewRequest request;

    @BeforeEach
    void setUp() {
        processor = new ReviewCreationProcessor(orderItemRepository, reviewRepository);
        buyer = new AppUser();
        buyer.setId(1L);
        buyer.setFullName("Buyer");
        Product product = new Product();
        product.setId(3L);
        Order order = new Order();
        order.setBuyer(buyer);
        order.setStatus(OrderStatus.COMPLETED);
        item = new OrderItem();
        item.setId(2L);
        item.setOrder(order);
        item.setProduct(product);
        item.setProductNameSnapshot("Product");
        request = new CreateReviewRequest();
        request.setOrderItemId(2L);
        request.setRating(5);
        request.setComment(" Great ");
    }

    @Test
    void completedOwnedOrderItemCreatesReviewFromOrderItemProduct() {
        when(orderItemRepository.findDetailedById(2L)).thenReturn(Optional.of(item));
        when(reviewRepository.existsByOrderItem_Id(2L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(9L);
            return review;
        });

        var response = processor.create(buyer, request);

        assertEquals(9L, response.getId());
        assertEquals(3L, response.getProductId());
        assertEquals("Great", response.getComment());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED", "SHIPPING", "CANCELLED"})
    void nonCompletedOrdersAreRejected(OrderStatus status) {
        item.getOrder().setStatus(status);
        when(orderItemRepository.findDetailedById(2L)).thenReturn(Optional.of(item));
        assertThrows(BadRequestException.class, () -> processor.create(buyer, request));
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void anotherBuyerCannotReviewOrderItem() {
        AppUser other = new AppUser();
        other.setId(99L);
        when(orderItemRepository.findDetailedById(2L)).thenReturn(Optional.of(item));
        assertThrows(ReviewAccessDeniedException.class, () -> processor.create(other, request));
    }

    @Test
    void missingOrderItemIsNotFound() {
        when(orderItemRepository.findDetailedById(2L)).thenReturn(Optional.empty());
        assertThrows(OrderItemNotFoundException.class, () -> processor.create(buyer, request));
    }

    @Test
    void alreadyReviewedOrderItemIsRejected() {
        when(orderItemRepository.findDetailedById(2L)).thenReturn(Optional.of(item));
        when(reviewRepository.existsByOrderItem_Id(2L)).thenReturn(true);
        assertThrows(ReviewConflictException.class, () -> processor.create(buyer, request));
    }
}
