package com.ecommerce.minishopee.review.service;

import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.review.dto.request.SellerReplyRequest;
import com.ecommerce.minishopee.review.entity.Review;
import com.ecommerce.minishopee.review.exception.ReviewAccessDeniedException;
import com.ecommerce.minishopee.review.exception.ReviewConflictException;
import com.ecommerce.minishopee.review.exception.ReviewNotFoundException;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerReviewServiceTests {
    @Mock ReviewRepository reviewRepository;
    @Mock ShopRepository shopRepository;
    SellerReviewService service;
    AppUser seller;
    Shop shop;
    Review review;
    SellerReplyRequest request;

    @BeforeEach
    void setUp() {
        service = new SellerReviewService(reviewRepository, shopRepository);
        seller = new AppUser(); seller.setId(1L);
        AppUser buyer = new AppUser(); buyer.setId(2L); buyer.setFullName("Buyer");
        shop = new Shop(); shop.setId(3L); shop.setOwner(seller);
        Product product = new Product(); product.setId(4L); product.setShop(shop);
        OrderItem item = new OrderItem(); item.setId(5L); item.setProductNameSnapshot("Product");
        review = new Review(); review.setId(6L); review.setBuyer(buyer); review.setProduct(product); review.setOrderItem(item);
        request = new SellerReplyRequest(); request.setReply(" Cảm ơn bạn ");
        when(shopRepository.findByOwnerId(1L)).thenReturn(Optional.of(shop));
    }

    @Test
    void owningSellerCanReplyOnce() {
        when(reviewRepository.findDetailedByIdForUpdate(6L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        var response = service.reply(6L, seller, request);
        assertEquals("Cảm ơn bạn", response.getSellerReply());
        assertNotNull(response.getRepliedAt());
    }

    @Test
    void sellerFromAnotherShopIsRejected() {
        Shop other = new Shop(); other.setId(99L); review.getProduct().setShop(other);
        when(reviewRepository.findDetailedByIdForUpdate(6L)).thenReturn(Optional.of(review));
        assertThrows(ReviewAccessDeniedException.class, () -> service.reply(6L, seller, request));
    }

    @Test
    void secondReplyIsRejected() {
        review.setSellerReply("Existing");
        when(reviewRepository.findDetailedByIdForUpdate(6L)).thenReturn(Optional.of(review));
        assertThrows(ReviewConflictException.class, () -> service.reply(6L, seller, request));
    }

    @Test
    void missingReviewIsNotFound() {
        when(reviewRepository.findDetailedByIdForUpdate(6L)).thenReturn(Optional.empty());
        assertThrows(ReviewNotFoundException.class, () -> service.reply(6L, seller, request));
    }
}
