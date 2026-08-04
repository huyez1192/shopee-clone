package com.ecommerce.minishopee.shop.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectShopRequest {

    @NotBlank(message = "Lý do từ chối không được bỏ trống.")
    private String reason;
}
