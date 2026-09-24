package com.ecommerce.minishopee.upload.service;

import com.ecommerce.minishopee.upload.exception.ImageUploadUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "cloudinary.enabled", havingValue = "false", matchIfMissing = true)
public class UnavailableCloudinaryImageClient implements CloudinaryImageClient {
    @Override
    public Map<String, Object> upload(byte[] content, Map<String, Object> options) {
        throw new ImageUploadUnavailableException("Dịch vụ tải ảnh chưa được cấu hình.");
    }
}
