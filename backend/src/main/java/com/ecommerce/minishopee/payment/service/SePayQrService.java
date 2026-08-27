package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.payment.entity.Payment;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SePayQrService {

    private static final String VIET_QR_URL = "https://vietqr.app/img";

    public String generateQrUrl(
            Payment payment
    ) {
        return UriComponentsBuilder
                .fromUriString(VIET_QR_URL)
                .queryParam(
                        "acc",
                        payment.getAccountNumber()
                )
                .queryParam(
                        "bank",
                        payment.getBankCode()
                )
                .queryParam(
                        "amount",
                        payment.getAmount()
                                .toBigInteger()
                )
                .queryParam(
                        "des",
                        payment.getPaymentCode()
                )
                .queryParam(
                        "template",
                        "compact"
                )
                .build()
                .encode()
                .toUriString();
    }
}