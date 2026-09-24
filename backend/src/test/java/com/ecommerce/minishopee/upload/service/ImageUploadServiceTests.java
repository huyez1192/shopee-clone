package com.ecommerce.minishopee.upload.service;

import com.ecommerce.minishopee.upload.dto.response.ImageUploadResponse;
import com.ecommerce.minishopee.upload.enums.ImageUploadPurpose;
import com.ecommerce.minishopee.upload.exception.ImageUploadException;
import com.ecommerce.minishopee.upload.exception.InvalidImageException;
import com.ecommerce.minishopee.upload.exception.ImageUploadUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ImageUploadServiceTests {
    @Mock
    private CloudinaryImageClient cloudinaryImageClient;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService(cloudinaryImageClient);
    }

    @Test
    void uploadsJpegAndMapsCloudinaryResponse() throws IOException {
        when(cloudinaryImageClient.upload(any(byte[].class), anyMap())).thenReturn(successResult());

        ImageUploadResponse response = imageUploadService.uploadImage(
                image("photo.jpg", "image/jpeg"), ImageUploadPurpose.PRODUCT
        );

        assertEquals("https://res.cloudinary.com/test/image/upload/photo.jpg", response.secureUrl());
        assertEquals("mini-shopee/products/id", response.publicId());
        assertEquals(640, response.width());
        assertEquals(480, response.height());
        assertEquals("jpg", response.format());
        assertEquals(1234L, response.bytes());
    }

    @Test
    void uploadsPng() throws IOException {
        when(cloudinaryImageClient.upload(any(byte[].class), anyMap())).thenReturn(successResult());

        imageUploadService.uploadImage(image("photo.png", "image/png"), ImageUploadPurpose.PRODUCT);

        verify(cloudinaryImageClient).upload(any(byte[].class), anyMap());
    }

    @Test
    void uploadsWebp() throws IOException {
        when(cloudinaryImageClient.upload(any(byte[].class), anyMap())).thenReturn(successResult());

        imageUploadService.uploadImage(image("photo.webp", "image/webp"), ImageUploadPurpose.PRODUCT);

        verify(cloudinaryImageClient).upload(any(byte[].class), anyMap());
    }

    @Test
    void rejectsEmptyFileBeforeCloudinaryCall() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(InvalidImageException.class,
                () -> imageUploadService.uploadImage(file, ImageUploadPurpose.PRODUCT));
        verify(cloudinaryImageClient, never()).upload(any(byte[].class), anyMap());
    }

    @Test
    void rejectsNonImageMimeBeforeCloudinaryCall() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "text".getBytes());

        assertThrows(InvalidImageException.class,
                () -> imageUploadService.uploadImage(file, ImageUploadPurpose.PRODUCT));
        verify(cloudinaryImageClient, never()).upload(any(byte[].class), anyMap());
    }

    @Test
    void rejectsOversizedFileBeforeCloudinaryCall() throws IOException {
        byte[] content = new byte[(int) ImageUploadService.MAX_FILE_SIZE + 1];
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", content);

        assertThrows(InvalidImageException.class,
                () -> imageUploadService.uploadImage(file, ImageUploadPurpose.PRODUCT));
        verify(cloudinaryImageClient, never()).upload(any(byte[].class), anyMap());
    }

    @Test
    void translatesCloudinaryIoFailure() throws IOException {
        when(cloudinaryImageClient.upload(any(byte[].class), anyMap())).thenThrow(new IOException("secret detail"));

        ImageUploadException exception = assertThrows(ImageUploadException.class,
                () -> imageUploadService.uploadImage(image("photo.jpg", "image/jpeg"), ImageUploadPurpose.PRODUCT));

        assertEquals("Không thể tải ảnh lên dịch vụ lưu trữ.", exception.getMessage());
    }

    @Test
    void rejectsResponseWithoutSecureUrl() throws IOException {
        when(cloudinaryImageClient.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("public_id", "mini-shopee/products/id"));

        assertThrows(ImageUploadException.class,
                () -> imageUploadService.uploadImage(image("photo.jpg", "image/jpeg"), ImageUploadPurpose.PRODUCT));
    }

    @Test
    void rejectsResponseWithoutPublicId() throws IOException {
        when(cloudinaryImageClient.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/test/image/upload/photo.jpg"));

        assertThrows(ImageUploadException.class,
                () -> imageUploadService.uploadImage(image("photo.jpg", "image/jpeg"), ImageUploadPurpose.PRODUCT));
    }

    @Test
    void mapsProductPurposeToControlledFolder() throws IOException {
        assertFolder(ImageUploadPurpose.PRODUCT, "mini-shopee/products");
    }

    @Test
    void mapsShopLogoPurposeToControlledFolder() throws IOException {
        assertFolder(ImageUploadPurpose.SHOP_LOGO, "mini-shopee/shops");
    }

    @Test
    void mapsUserAvatarPurposeToControlledFolder() throws IOException {
        assertFolder(ImageUploadPurpose.USER_AVATAR, "mini-shopee/avatars");
    }

    @Test
    void rejectsMissingPurposeBeforeCloudinaryCall() throws IOException {
        assertThrows(InvalidImageException.class,
                () -> imageUploadService.uploadImage(image("photo.jpg", "image/jpeg"), null));
        verify(cloudinaryImageClient, never()).upload(any(byte[].class), anyMap());
    }

    @Test
    void reportsServiceUnavailableWhenCloudinaryIsDisabled() {
        ImageUploadService disabledService = new ImageUploadService(new UnavailableCloudinaryImageClient());

        ImageUploadUnavailableException exception = assertThrows(
                ImageUploadUnavailableException.class,
                () -> disabledService.uploadImage(
                        image("photo.jpg", "image/jpeg"),
                        ImageUploadPurpose.PRODUCT
                )
        );

        assertEquals("Dịch vụ tải ảnh chưa được cấu hình.", exception.getMessage());
    }

    private void assertFolder(ImageUploadPurpose purpose, String expectedFolder) throws IOException {
        when(cloudinaryImageClient.upload(any(byte[].class), anyMap())).thenReturn(successResult());
        ArgumentCaptor<Map<String, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);

        imageUploadService.uploadImage(image("photo.jpg", "image/jpeg"), purpose);

        verify(cloudinaryImageClient).upload(any(byte[].class), optionsCaptor.capture());
        Map<String, Object> options = optionsCaptor.getValue();
        assertEquals(expectedFolder, options.get("folder"));
        assertEquals("image", options.get("resource_type"));
        assertTrue(options.get("public_id") instanceof String);
    }

    private MockMultipartFile image(String filename, String contentType) {
        return new MockMultipartFile("file", filename, contentType, new byte[]{1, 2, 3});
    }

    private Map<String, Object> successResult() {
        return Map.of(
                "secure_url", "https://res.cloudinary.com/test/image/upload/photo.jpg",
                "public_id", "mini-shopee/products/id",
                "width", 640,
                "height", 480,
                "format", "jpg",
                "bytes", 1234L
        );
    }
}
