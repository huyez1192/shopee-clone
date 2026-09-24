package com.ecommerce.minishopee.upload.controller;

import com.ecommerce.minishopee.upload.dto.response.ImageUploadResponse;
import com.ecommerce.minishopee.upload.enums.ImageUploadPurpose;
import com.ecommerce.minishopee.upload.service.ImageUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class ImageUploadController {
    private final ImageUploadService imageUploadService;

    public ImageUploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(value = "/images", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUploadResponse uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("purpose") ImageUploadPurpose purpose
    ) {
        return imageUploadService.uploadImage(file, purpose);
    }
}
