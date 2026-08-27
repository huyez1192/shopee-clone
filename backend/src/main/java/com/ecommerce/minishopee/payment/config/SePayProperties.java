package com.ecommerce.minishopee.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sepay")
public class SePayProperties {

    private String bankCode;

    private String accountNumber;

    private String accountName;

    private String webhookSecret;
}