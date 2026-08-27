package com.ecommerce.minishopee.payment.scheduler;

import com.ecommerce.minishopee.payment.service.PaymentExpirationService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentExpirationScheduler {

    private final PaymentExpirationService
            paymentExpirationService;

    public PaymentExpirationScheduler(
            PaymentExpirationService
                    paymentExpirationService
    ) {
        this.paymentExpirationService =
                paymentExpirationService;
    }

    /*
     * Mỗi 60 giây kiểm tra Payment hết hạn.
     */
    @Scheduled(fixedDelay = 60_000)
    public void expirePendingPayments() {

        List<Long> expiredPaymentIds =
                paymentExpirationService
                        .findExpiredPaymentIds();

        for (Long paymentId : expiredPaymentIds) {
            try {
                paymentExpirationService
                        .expirePayment(
                                paymentId
                        );
            } catch (Exception exception) {
                System.err.println(
                        "Không thể expire Payment "
                                + paymentId
                                + ": "
                                + exception.getMessage()
                );
            }
        }
    }
}