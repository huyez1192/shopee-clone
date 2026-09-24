package com.ecommerce.minishopee.upload.dto.response;

public record ImageUploadResponse(
        String secureUrl,
        String publicId,
        Integer width,
        Integer height,
        String format,
        Long bytes
) {
}
