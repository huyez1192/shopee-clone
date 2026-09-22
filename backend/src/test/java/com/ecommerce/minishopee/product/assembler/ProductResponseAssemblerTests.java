package com.ecommerce.minishopee.product.assembler;

import com.ecommerce.minishopee.category.entity.Category;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.repository.ProductVariantRepository;
import com.ecommerce.minishopee.review.repository.ProductRatingProjection;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductResponseAssemblerTests {
    @Mock ProductVariantRepository variantRepository;
    @Mock ReviewRepository reviewRepository;

    @Test
    void productListUsesOneBatchRatingAggregate() {
        Product first = product(1L);
        Product second = product(2L);
        ProductRatingProjection row = mock(ProductRatingProjection.class);
        when(row.getProductId()).thenReturn(1L);
        when(row.getAverageRating()).thenReturn(4.0);
        when(row.getReviewCount()).thenReturn(3L);
        when(reviewRepository.summarizeByProductIds(anyCollection())).thenReturn(List.of(row));
        when(variantRepository.findAllByProduct_IdInAndStatusIn(anyCollection(), anyCollection()))
                .thenReturn(List.of());

        var result = new ProductResponseAssembler(variantRepository, reviewRepository)
                .toPublicResponses(List.of(first, second));

        assertEquals(4.0, result.get(0).getAverageRating());
        assertEquals(3L, result.get(0).getReviewCount());
        assertEquals(0.0, result.get(1).getAverageRating());
        assertEquals(0L, result.get(1).getReviewCount());
        verify(reviewRepository, times(1)).summarizeByProductIds(anyCollection());
    }

    private Product product(Long id) {
        Category category = new Category(); category.setId(10L); category.setName("Category");
        Shop shop = new Shop(); shop.setId(20L); shop.setName("Shop");
        Product product = new Product(); product.setId(id); product.setName("P" + id);
        product.setCategory(category); product.setShop(shop);
        return product;
    }
}
