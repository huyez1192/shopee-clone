package com.example.productcrudapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống.")
    private String name;

    private String description;

    @NotNull(message = "Giá sản phẩm không được rỗng.")
    @DecimalMin(value = "0.01", message = "Giá tiền phải lớn hơn 0.")
    private BigDecimal price;

    @NotNull(message = "Số lượng trong kho không được rỗng.")
    @Min(value = 0, message = "Số lượng trong kho phải >= 0")
    private Integer stockQuantity;

    @NotBlank(message = "Category không được để trống")
    private String category;

    private String imageUrl;

}
