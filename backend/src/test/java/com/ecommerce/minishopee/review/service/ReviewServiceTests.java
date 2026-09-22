package com.ecommerce.minishopee.review.service;

import com.ecommerce.minishopee.product.repository.ProductRepository;
import com.ecommerce.minishopee.review.dto.request.CreateReviewRequest;
import com.ecommerce.minishopee.review.entity.Review;
import com.ecommerce.minishopee.review.exception.ReviewConflictException;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTests {
    @Mock ReviewCreationProcessor processor;
    @Mock ReviewRepository reviewRepository;
    @Mock ProductRepository productRepository;

    @Test
    void concurrentOrderItemUniqueCollisionBecomesConflict() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setOrderItemId(10L);
        when(processor.create(any(), same(request)))
                .thenThrow(new DataIntegrityViolationException("uk_reviews_order_item_id"));
        when(reviewRepository.findByOrderItem_Id(10L)).thenReturn(Optional.of(new Review()));

        ReviewService service = new ReviewService(processor, reviewRepository, productRepository);
        assertThrows(ReviewConflictException.class, () -> service.createReview(null, request));
    }

    @Test
    void unrelatedIntegrityViolationIsNotSwallowed() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setOrderItemId(10L);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("other constraint");
        when(processor.create(any(), same(request))).thenThrow(failure);

        ReviewService service = new ReviewService(processor, reviewRepository, productRepository);
        assertSame(failure, assertThrows(DataIntegrityViolationException.class,
                () -> service.createReview(null, request)));
    }

    @Test
    void publicReviewsArePagedAndIncludeReplyWithoutSensitiveFields() {
        var buyer = new com.ecommerce.minishopee.user.entity.AppUser();
        buyer.setFullName("Buyer"); buyer.setEmail("private@example.com"); buyer.setPhone("secret");
        var product = new com.ecommerce.minishopee.product.entity.Product(); product.setId(1L);
        var item = new com.ecommerce.minishopee.order.entity.OrderItem();
        item.setId(2L); item.setProductNameSnapshot("Product");
        Review review = new Review(); review.setId(3L); review.setBuyer(buyer);
        review.setProduct(product); review.setOrderItem(item); review.setSellerReply("Thanks");
        when(productRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findDetailedByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(review)));

        var result = new ReviewService(processor, reviewRepository, productRepository)
                .getProductReviews(1L, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals("Thanks", result.getContent().get(0).getSellerReply());
        assertEquals("Buyer", result.getContent().get(0).getBuyerDisplayName());
    }

    @Test
    void productWithoutReviewsReturnsEmptyPage() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findDetailedByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        var result = new ReviewService(processor, reviewRepository, productRepository)
                .getProductReviews(1L, 0, 10);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }
}
