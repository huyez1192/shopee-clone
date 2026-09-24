package com.ecommerce.minishopee.payment.controller;

import com.ecommerce.minishopee.payment.repository.SePayTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SePayWebhookControllerIntegrationTests {
    private static final String SECRET = "test-only-sepay-webhook-secret";

    @Autowired MockMvc mockMvc;
    @Autowired SePayTransactionRepository transactionRepository;

    @Test
    void missingSignatureIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/webhooks/sepay")
                        .header("X-SePay-Timestamp", currentTimestamp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(31001L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingOrStaleTimestampIsUnauthorized() throws Exception {
        String body = payload(31002L);
        mockMvc.perform(post("/api/webhooks/sepay")
                        .header("X-SePay-Signature", "sha256=invalid")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        String stale = String.valueOf(Instant.now().minusSeconds(301).getEpochSecond());
        mockMvc.perform(post("/api/webhooks/sepay")
                        .header("X-SePay-Timestamp", stale)
                        .header("X-SePay-Signature", signature(stale, body))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSignatureIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/webhooks/sepay")
                        .header("X-SePay-Timestamp", currentTimestamp())
                        .header("X-SePay-Signature", "sha256=0000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(31003L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validHmacProcessesExactRawBody() throws Exception {
        String body = payload(31004L);
        String timestamp = currentTimestamp();

        mockMvc.perform(post("/api/webhooks/sepay")
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", signature(timestamp, body))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        assertEquals(1, transactionRepository.count());
    }

    @Test
    void bodyTamperingInvalidatesExistingSignature() throws Exception {
        String signedBody = payload(31005L);
        String modifiedBody = payload(31006L);
        String timestamp = currentTimestamp();

        mockMvc.perform(post("/api/webhooks/sepay")
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", signature(timestamp, signedBody))
                        .contentType(MediaType.APPLICATION_JSON).content(modifiedBody))
                .andExpect(status().isUnauthorized());
    }

    private static String payload(long id) {
        return "{\"id\":" + id + ",\"gateway\":\"TEST_BANK\",\"accountNumber\":\"0000000000\","
                + "\"transferType\":\"out\",\"transferAmount\":1000,\"referenceCode\":\"REF-" + id + "\"}";
    }

    private static String currentTimestamp() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    private static String signature(String timestamp, String rawBody) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
        return "sha256=" + HexFormat.of().formatHex(hash);
    }
}
