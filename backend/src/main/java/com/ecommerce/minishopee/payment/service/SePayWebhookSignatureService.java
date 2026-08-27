package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.payment.config.SePayProperties;
import com.ecommerce.minishopee.common.exception.BadRequestException;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class SePayWebhookSignatureService {

    private static final long MAX_TIMESTAMP_DIFF_SECONDS = 300;

    private final SePayProperties sePayProperties;

    public SePayWebhookSignatureService(SePayProperties sePayProperties) {
        this.sePayProperties = sePayProperties;
    }

    public boolean isValid(
            String signature,
            String timestamp,
            String rawBody
    ) {
        if (
                signature == null
                        || timestamp == null
                        || rawBody == null
        ) {
            return false;
        }

        long timestampSeconds;

        try {
            timestampSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            return false;
        }

        long now = Instant.now().getEpochSecond();

        /*
         * Chống replay attack.
         */
        if (
                Math.abs(
                        now - timestampSeconds
                ) > MAX_TIMESTAMP_DIFF_SECONDS
        ) {
            return false;
        }

        String secret =
                sePayProperties
                        .getWebhookSecret();

        if (
                secret == null
                        || secret.isBlank()
        ) {
            throw new IllegalStateException(
                    "Chưa cấu hình SEPAY_WEBHOOK_SECRET."
            );
        }

        try {
            String signedContent =
                    timestamp
                            + "."
                            + rawBody;

            Mac mac =
                    Mac.getInstance(
                            "HmacSHA256"
                    );

            SecretKeySpec key =
                    new SecretKeySpec(
                            secret.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            "HmacSHA256"
                    );

            mac.init(key);

            byte[] hash =
                    mac.doFinal(
                            signedContent.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            String expectedSignature =
                    "sha256="
                            + HexFormat.of()
                            .formatHex(hash);

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(
                            StandardCharsets.UTF_8
                    ),
                    signature.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Không thể xác thực chữ ký SePay.",
                    exception
            );
        }
    }
}