package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.payment.dto.request.SePayWebhookRequest;
import com.ecommerce.minishopee.payment.entity.SePayTransaction;
import com.ecommerce.minishopee.payment.repository.SePayTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SePayWebhookService {

    private final SePayWebhookProcessor webhookProcessor;
    private final SePayTransactionRepository transactionRepository;

    public SePayWebhookService(
            SePayWebhookProcessor webhookProcessor,
            SePayTransactionRepository transactionRepository
    ) {
        this.webhookProcessor = webhookProcessor;
        this.transactionRepository = transactionRepository;
    }

    public void process(SePayWebhookRequest request, String rawPayload) {
        if (request.getId() == null) {
            throw new BadRequestException("Webhook SePay không có transaction id.");
        }

        try {
            webhookProcessor.processFirstDelivery(request, rawPayload);
        } catch (DataIntegrityViolationException exception) {
            SePayTransaction existing = transactionRepository
                    .findBySePayTransactionId(request.getId())
                    .orElseThrow(() -> exception);

            if (!isSameDelivery(existing, request, rawPayload)) {
                throw exception;
            }
        }
    }

    private boolean isSameDelivery(
            SePayTransaction existing,
            SePayWebhookRequest request,
            String rawPayload
    ) {
        return Objects.equals(existing.getSePayTransactionId(), request.getId())
                && Objects.equals(existing.getGateway(), request.getGateway())
                && Objects.equals(existing.getAccountNumber(), request.getAccountNumber())
                && amountsEqual(existing, request)
                && Objects.equals(existing.getReferenceCode(), request.getReferenceCode())
                && Objects.equals(existing.getRawPayload(), rawPayload);
    }

    private boolean amountsEqual(
            SePayTransaction existing,
            SePayWebhookRequest request
    ) {
        if (existing.getTransferAmount() == null || request.getTransferAmount() == null) {
            return existing.getTransferAmount() == request.getTransferAmount();
        }

        return existing.getTransferAmount().compareTo(request.getTransferAmount()) == 0;
    }
}
