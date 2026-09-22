package com.ecommerce.minishopee.product.service;

import com.ecommerce.minishopee.product.assembler.ProductResponseAssembler;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRatingSortTests {
    @Mock ProductRepository productRepository;
    @Mock ProductResponseAssembler assembler;

    @Test
    void ratingDescUsesDatabasePagedAggregateQuery() {
        Product product = new Product(); product.setId(1L);
        when(productRepository.findPublicProductsPageByRatingDesc(any(), any(), any(), any(), anyCollection(), any()))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(assembler.toPublicResponses(anyList())).thenReturn(List.of());

        var result = new ProductService(productRepository, assembler)
                .getProductsWithPaginationAndSorting(0, 10, "rating_desc", "desc");

        assertEquals(1, result.getTotalElements());
        verify(productRepository).findPublicProductsPageByRatingDesc(any(), any(), any(), any(), anyCollection(), any());
        verify(productRepository, never()).findPublicProductsPage(any(), any(), any(), any(), anyCollection(), any());
    }
}
