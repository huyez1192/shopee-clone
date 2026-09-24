package com.ecommerce.minishopee.payment.scheduler;

import com.ecommerce.minishopee.payment.service.PaymentExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(PaymentExpirationScheduler.class);

    private final PaymentExpirationService paymentExpirationService;

    public PaymentExpirationScheduler(PaymentExpirationService paymentExpirationService) {
        this.paymentExpirationService = paymentExpirationService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void expirePendingPayments() {
        List<Long> expiredPaymentIds = paymentExpirationService.findExpiredPaymentIds();

        for (Long paymentId : expiredPaymentIds) {
            try {
                paymentExpirationService.expirePayment(paymentId);
            } catch (Exception exception) {
                log.error("Cannot expire Payment: paymentId={}", paymentId, exception);
            }
        }
    }
}
