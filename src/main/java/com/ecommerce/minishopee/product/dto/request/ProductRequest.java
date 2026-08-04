package com.ecommerce.minishopee.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống.")
    private String name;

    private String description;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    private String imageUrl;

}
