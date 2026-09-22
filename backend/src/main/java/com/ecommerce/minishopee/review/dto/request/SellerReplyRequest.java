package com.ecommerce.minishopee.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerReplyRequest {
    @NotBlank(message = "reply không được để trống.")
    @Size(max = 2000, message = "reply không được vượt quá 2000 ký tự.")
    private String reply;
}
