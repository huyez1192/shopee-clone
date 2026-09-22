package com.ecommerce.minishopee.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewRequest {
    @NotNull(message = "orderItemId không được để trống.")
    private Long orderItemId;

    @NotNull(message = "rating không được để trống.")
    @Min(value = 1, message = "rating phải từ 1 đến 5.")
    @Max(value = 5, message = "rating phải từ 1 đến 5.")
    private Integer rating;

    @Size(max = 2000, message = "comment không được vượt quá 2000 ký tự.")
    private String comment;
}
