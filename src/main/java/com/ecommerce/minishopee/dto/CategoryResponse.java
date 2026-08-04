package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.CategoryStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CategoryResponse {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private CategoryStatus status;

    private Long parentId;

    private String parentName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
