package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống.")
    private String name;

    @NotBlank(message = "Slug danh mục không được để trống.")
    private String slug;

    private String description;

    private CategoryStatus status;

    private Long parentId;
}
