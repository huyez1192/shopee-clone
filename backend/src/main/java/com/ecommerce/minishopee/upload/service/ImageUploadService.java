package com.ecommerce.minishopee.upload.service;

import com.ecommerce.minishopee.upload.dto.response.ImageUploadResponse;
import com.ecommerce.minishopee.upload.enums.ImageUploadPurpose;
import com.ecommerce.minishopee.upload.exception.ImageUploadException;
import com.ecommerce.minishopee.upload.exception.InvalidImageException;
import com.ecommerce.minishopee.upload.exception.ImageUploadUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageUploadService {
    static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final CloudinaryImageClient cloudinaryImageClient;

    public ImageUploadService(CloudinaryImageClient cloudinaryImageClient) {
        this.cloudinaryImageClient = cloudinaryImageClient;
    }

    public ImageUploadResponse uploadImage(MultipartFile file, ImageUploadPurpose purpose) {
        validate(file, purpose);

        Map<String, Object> options = Map.of(
                "folder", purpose.getFolder(),
                "public_id", UUID.randomUUID().toString(),
                "resource_type", "image"
        );

        try {
            Map<String, Object> result = cloudinaryImageClient.upload(file.getBytes(), options);
            return mapResult(result);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ImageUploadException imageUploadException) {
                throw imageUploadException;
            }
            if (exception instanceof ImageUploadUnavailableException unavailableException) {
                throw unavailableException;
            }
            throw new ImageUploadException("Không thể tải ảnh lên dịch vụ lưu trữ.", exception);
        }
    }

    private void validate(MultipartFile file, ImageUploadPurpose purpose) {
        if (purpose == null) {
            throw new InvalidImageException("Mục đích tải ảnh không hợp lệ.");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Tệp ảnh không được để trống.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException("Tệp ảnh không được vượt quá 5 MB.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidImageException("Định dạng ảnh không hợp lệ. Chỉ hỗ trợ JPEG, PNG và WEBP.");
        }
    }

    private ImageUploadResponse mapResult(Map<String, Object> result) {
        String secureUrl = stringValue(result.get("secure_url"));
        String publicId = stringValue(result.get("public_id"));

        if (!isHttpsUrl(secureUrl) || publicId == null || publicId.isBlank()) {
            throw new ImageUploadException("Dịch vụ lưu trữ trả về kết quả không hợp lệ.");
        }

        return new ImageUploadResponse(
                secureUrl,
                publicId,
                integerValue(result.get("width")),
                integerValue(result.get("height")),
                stringValue(result.get("format")),
                longValue(result.get("bytes"))
        );
    }

    private boolean isHttpsUrl(String value) {
        if (value == null) return false;
        try {
            return "https".equalsIgnoreCase(URI.create(value).getScheme());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String stringValue(Object value) {
        return value instanceof String string ? string : null;
    }

    private Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
