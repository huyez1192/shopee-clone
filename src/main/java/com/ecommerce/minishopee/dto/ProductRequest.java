package com.ecommerce.minishopee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

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
