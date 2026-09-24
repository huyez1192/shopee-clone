package com.ecommerce.minishopee.payment.controller;

import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentProvider;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.security.jwt.JwtService;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentOwnershipIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired JwtService jwtService;

    private Payment payment;
    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        AppUser owner = user("payment-owner@example.test");
        AppUser other = user("payment-other@example.test");
        ownerToken = jwtService.generateToken(owner);
        otherToken = jwtService.generateToken(other);

        payment = new Payment();
        payment.setPaymentCode("MS-OWNER-TEST");
        payment.setUser(owner);
        payment.setProvider(PaymentProvider.SEPAY);
        payment.setStatus(PaymentSessionStatus.PENDING);
        payment.setAmount(BigDecimal.valueOf(150_000));
        payment.setBankCode("TEST_BANK");
        payment.setAccountNumber("0000000000");
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        paymentRepository.saveAndFlush(payment);
    }

    @Test
    void ownerCanLoadPaymentDetailAndStatus() throws Exception {
        mockMvc.perform(get("/api/payments/{code}", payment.getPaymentCode()).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentCode").value(payment.getPaymentCode()))
                .andExpect(jsonPath("$.provider").value("SEPAY"))
                .andExpect(jsonPath("$.qrUrl").isNotEmpty());
        mockMvc.perform(get("/api/payments/{code}/status", payment.getPaymentCode()).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
    }

    @Test
    void nonOwnerCannotLoadPaymentDetailOrStatus() throws Exception {
        mockMvc.perform(get("/api/payments/{code}", payment.getPaymentCode()).header("Authorization", bearer(otherToken)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/payments/{code}/status", payment.getPaymentCode()).header("Authorization", bearer(otherToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousCannotLoadPaymentDetailOrStatus() throws Exception {
        mockMvc.perform(get("/api/payments/{code}", payment.getPaymentCode())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/payments/{code}/status", payment.getPaymentCode())).andExpect(status().isUnauthorized());
    }

    private AppUser user(String email) {
        AppUser user = new AppUser();
        user.setFullName("Payment User"); user.setEmail(email); user.setPassword("password");
        user.setPhone(email); user.setRole(Role.USER); user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
