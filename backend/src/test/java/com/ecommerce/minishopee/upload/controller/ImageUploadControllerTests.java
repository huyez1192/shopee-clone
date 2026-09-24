package com.ecommerce.minishopee.upload.controller;

import com.ecommerce.minishopee.common.exception.GlobalExceptionHandler;
import com.ecommerce.minishopee.upload.service.ImageUploadService;
import com.ecommerce.minishopee.upload.exception.ImageUploadUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImageUploadControllerTests {
    @Mock
    private ImageUploadService imageUploadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ImageUploadController(imageUploadService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsArbitraryPurposeValue() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/uploads/images")
                        .file(file)
                        .param("purpose", "../../arbitrary-folder"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Tham số yêu cầu không hợp lệ hoặc bị thiếu."));

        verifyNoInteractions(imageUploadService);
    }

    @Test
    void returnsServiceUnavailableWhenCloudinaryIsDisabled() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        when(imageUploadService.uploadImage(any(), any())).thenThrow(
                new ImageUploadUnavailableException("Dịch vụ tải ảnh chưa được cấu hình.")
        );

        mockMvc.perform(multipart("/api/uploads/images")
                        .file(file)
                        .param("purpose", "PRODUCT"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Dịch vụ tải ảnh chưa được cấu hình."));
    }
}
