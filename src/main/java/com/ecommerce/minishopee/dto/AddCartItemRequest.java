package com.ecommerce.minishopee.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCartItemRequest {

    @NotNull(message = "variantId không được để trống.")
    private Long variantId;

    @NotNull(message = "Số lượng không được để trống.")
    @Min(value = 1, message = "Số lượng phải >= 1")
    private Integer quantity;
}
