package com.ecommerce.minishopee.payment.controller;

import com.ecommerce.minishopee.payment.dto.request.SePayWebhookRequest;
import com.ecommerce.minishopee.payment.service.SePayWebhookService;
import com.ecommerce.minishopee.payment.service.SePayWebhookSignatureService;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/sepay")
public class SePayWebhookController {

    private final SePayWebhookSignatureService signatureService;

    private final SePayWebhookService webhookService;

    private final JsonMapper jsonMapper;

    public SePayWebhookController(
            SePayWebhookSignatureService signatureService,
            SePayWebhookService webhookService,
            JsonMapper jsonMapper
    ) {
        this.signatureService = signatureService;
        this.webhookService = webhookService;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @RequestHeader(
                    value = "X-SePay-Signature",
                    required = false
            )
            String signature,

            @RequestHeader(
                    value = "X-SePay-Timestamp",
                    required = false
            )
            String timestamp,

            @RequestBody String rawBody
    ) throws Exception {

        boolean valid =
                signatureService.isValid(
                        signature,
                        timestamp,
                        rawBody
                );

        if (!valid) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "success",
                                    false,
                                    "message",
                                    "Invalid SePay signature"
                            )
                    );
        }

        SePayWebhookRequest request =
                jsonMapper.readValue(
                        rawBody,
                        SePayWebhookRequest.class
                );

        webhookService.process(
                request,
                rawBody
        );

        return ResponseEntity.ok(
                Map.of(
                        "success",
                        true
                )
        );
    }
}