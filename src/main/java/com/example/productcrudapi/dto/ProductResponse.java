package com.example.productcrudapi.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Builder
public class ProductResponse {
    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stockQuantity;

    private String category;

    private String imageUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
