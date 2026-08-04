package com.ecommerce.minishopee.category.mapper;

import com.ecommerce.minishopee.category.dto.request.CategoryRequest;
import com.ecommerce.minishopee.category.dto.response.CategoryResponse;
import com.ecommerce.minishopee.category.entity.Category;

public class CategoryMapper {

    public static Category toEntity(CategoryRequest request) {
        Category category = new Category();

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());

        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        return category;
    }

    public static CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .status(category.getStatus())
                .parentId(
                        category.getParent() != null
                                ? category.getParent().getId()
                                : null
                )
                .parentName(
                        category.getParent() != null
                                ? category.getParent().getName()
                                : null
                )
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public static void updateEntity(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());

        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
    }
}
