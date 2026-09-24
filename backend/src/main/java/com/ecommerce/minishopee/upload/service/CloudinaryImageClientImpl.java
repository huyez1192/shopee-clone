package com.ecommerce.minishopee.upload.service;

import com.cloudinary.Cloudinary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "cloudinary.enabled", havingValue = "true")
public class CloudinaryImageClientImpl implements CloudinaryImageClient {
    private final Cloudinary cloudinary;

    public CloudinaryImageClientImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> upload(byte[] content, Map<String, Object> options) throws IOException {
        return cloudinary.uploader().upload(content, options);
    }
}
