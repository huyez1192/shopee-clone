package com.ecommerce.minishopee.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudinaryConfigTests {

    @Test
    void failsFastWithClearMessageWhenEnabledCredentialsAreIncomplete() {
        CloudinaryConfig config = new CloudinaryConfig();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> config.cloudinary("", "api-key", "api-secret")
        );

        assertEquals(
                "Cloudinary is enabled, but CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, "
                        + "and CLOUDINARY_API_SECRET must all be configured.",
                exception.getMessage()
        );
    }
}
