package com.ecommerce.minishopee.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantRequest {

    private String variantName1;

    private String variantValue1;

    private String variantName2;

    private String variantValue2;

    @NotNull(message = "Giá SKU không được để trống.")
    @DecimalMin(value = "0.01", message = "Giá SKU phải lớn hơn 0.")
    private BigDecimal price;

    @NotNull(message = "Tồn kho không được để trống.")
    @Min(value = 0, message = "Tồn kho phải lớn hơn hoặc bằng 0.")
    private Integer stockQuantity;

    private String skuCode;
}