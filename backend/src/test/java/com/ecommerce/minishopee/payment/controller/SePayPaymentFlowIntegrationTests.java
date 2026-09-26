package com.ecommerce.minishopee.payment.controller;

import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.order.enums.PaymentMethod;
import com.ecommerce.minishopee.order.enums.PaymentStatus;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.payment.dto.request.SePayWebhookRequest;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentProvider;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.payment.service.SePayWebhookService;
import com.ecommerce.minishopee.security.jwt.JwtService;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SePayPaymentFlowIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired SePayWebhookService webhookService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired AppUserRepository userRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired JwtService jwtService;

    @Test
    void webhookContentUpdatesDatabaseAndOwnerStatusEndpointReturnsPaid() throws Exception {
        String suffix = UUID.randomUUID().toString();
        AppUser buyer = saveUser("buyer-" + suffix + "@test.local", Role.USER);
        AppUser seller = saveUser("seller-" + suffix + "@test.local", Role.SELLER);
        Shop shop = saveShop(seller, suffix);
        Payment payment = savePendingPayment(buyer, suffix);
        Order order = savePendingOrder(buyer, shop, payment, suffix);

        SePayWebhookRequest webhook = new SePayWebhookRequest();
        webhook.setId(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        webhook.setGateway("TEST_BANK");
        webhook.setTransferType("in");
        webhook.setCode(null);
        webhook.setContent("Thanh toan " + payment.getPaymentCode() + " cho don hang");
        webhook.setAccountNumber(payment.getAccountNumber());
        webhook.setTransferAmount(payment.getAmount());
        webhook.setReferenceCode("REF-" + suffix);

        webhookService.process(webhook, "{\"id\":" + webhook.getId() + "}");

        Payment persistedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        Order persistedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentSessionStatus.PAID, persistedPayment.getStatus());
        assertNotNull(persistedPayment.getPaidAt());
        assertEquals(PaymentStatus.PAID, persistedOrder.getPaymentStatus());

        String ownerToken = jwtService.generateToken(buyer);
        mockMvc.perform(get("/api/payments/{paymentCode}/status", payment.getPaymentCode())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    private AppUser saveUser(String email, Role role) {
        AppUser user = new AppUser();
        user.setFullName(role.name());
        user.setEmail(email);
        user.setPassword("password");
        user.setPhone(email);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    private Shop saveShop(AppUser seller, String suffix) {
        Shop shop = new Shop();
        shop.setOwner(seller);
        shop.setName("Shop " + suffix);
        shop.setAddress("Address");
        shop.setContactPhone("0123456789");
        shop.setStatus(ShopStatus.APPROVED);
        return shopRepository.saveAndFlush(shop);
    }

    private Payment savePendingPayment(AppUser buyer, String suffix) {
        Payment payment = new Payment();
        payment.setPaymentCode("MS" + suffix.replace("-", "").toUpperCase());
        payment.setUser(buyer);
        payment.setProvider(PaymentProvider.SEPAY);
        payment.setAmount(new BigDecimal("500000"));
        payment.setBankCode("TEST_BANK");
        payment.setAccountNumber("123456");
        payment.setStatus(PaymentSessionStatus.PENDING);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        return paymentRepository.saveAndFlush(payment);
    }

    private Order savePendingOrder(AppUser buyer, Shop shop, Payment payment, String suffix) {
        Order order = new Order();
        order.setOrderCode("ORDER-" + suffix);
        order.setBuyer(buyer);
        order.setShop(shop);
        order.setPayment(payment);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.SEPAY);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setMerchandiseSubtotal(new BigDecimal("490000"));
        order.setShippingFee(new BigDecimal("10000"));
        order.setTotalAmount(new BigDecimal("500000"));
        order.setRecipientName("Buyer");
        order.setRecipientPhone("0123456789");
        order.setProvince("Province");
        order.setWard("Ward");
        order.setDetailAddress("Address");
        return orderRepository.saveAndFlush(order);
    }
}
