package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.*;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.payment.dto.request.SePayWebhookRequest;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.entity.SePayTransaction;
import com.ecommerce.minishopee.payment.enums.PaymentProvider;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.payment.repository.SePayTransactionRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SePayIdempotencyIntegrationTests {
    @Autowired SePayWebhookService webhookService;
    @Autowired SePayTransactionRepository transactionRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired AppUserRepository userRepository;
    @Autowired ShopRepository shopRepository;

    @Test
    void databaseRejectsDuplicateSePayTransactionIdOnFlush() {
        long transactionId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        transactionRepository.saveAndFlush(transaction(transactionId, "first"));
        assertThrows(DataIntegrityViolationException.class,
                () -> transactionRepository.saveAndFlush(transaction(transactionId, "second")));
        assertEquals(1, transactionRepository.findAll().stream()
                .filter(row -> row.getSePayTransactionId().equals(transactionId)).count());
    }

    @Test
    void exactReplayIsSuccessfulAndDoesNotRepeatPersistedSideEffects() {
        Fixture fixture = paymentFixture();
        SePayWebhookRequest request = request(fixture.transactionId(), fixture.payment().getPaymentCode(),
                fixture.payment().getAccountNumber(), fixture.payment().getAmount());
        String payload = "{\"id\":" + fixture.transactionId() + "}";

        webhookService.process(request, payload);
        assertDoesNotThrow(() -> webhookService.process(request, payload));

        Payment payment = paymentRepository.findById(fixture.payment().getId()).orElseThrow();
        Order order = orderRepository.findById(fixture.order().getId()).orElseThrow();
        assertEquals(PaymentSessionStatus.PAID, payment.getStatus());
        assertNotNull(payment.getPaidAt());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(1, transactionRepository.findAll().stream()
                .filter(row -> row.getSePayTransactionId().equals(fixture.transactionId())).count());
    }

    @Test
    void replayWithSameTransactionIdButDifferentPayloadIsRejected() {
        Fixture fixture = paymentFixture();
        SePayWebhookRequest request = request(fixture.transactionId(), fixture.payment().getPaymentCode(),
                fixture.payment().getAccountNumber(), fixture.payment().getAmount());
        webhookService.process(request, "original");
        assertThrows(DataIntegrityViolationException.class,
                () -> webhookService.process(request, "changed"));
        assertEquals(1, transactionRepository.findAll().stream()
                .filter(row -> row.getSePayTransactionId().equals(fixture.transactionId())).count());
    }

    private Fixture paymentFixture() {
        String suffix = UUID.randomUUID().toString();
        AppUser buyer = user("buyer-" + suffix + "@test.local", Role.USER);
        AppUser seller = user("seller-" + suffix + "@test.local", Role.SELLER);
        Shop shop = new Shop(); shop.setOwner(seller); shop.setName("Shop " + suffix);
        shop.setAddress("Address"); shop.setContactPhone("0123456789"); shop.setStatus(ShopStatus.APPROVED);
        shop = shopRepository.saveAndFlush(shop);
        Payment payment = new Payment(); payment.setPaymentCode("PAY-" + suffix); payment.setUser(buyer);
        payment.setProvider(PaymentProvider.SEPAY); payment.setAmount(new BigDecimal("500000"));
        payment.setAccountNumber("123456"); payment.setStatus(PaymentSessionStatus.PENDING);
        payment = paymentRepository.saveAndFlush(payment);
        Order order = new Order(); order.setOrderCode("ORDER-" + suffix); order.setBuyer(buyer); order.setShop(shop);
        order.setPayment(payment); order.setStatus(OrderStatus.PENDING); order.setPaymentMethod(PaymentMethod.SEPAY);
        order.setPaymentStatus(PaymentStatus.UNPAID); order.setMerchandiseSubtotal(new BigDecimal("490000"));
        order.setShippingFee(new BigDecimal("10000")); order.setTotalAmount(new BigDecimal("500000"));
        order.setRecipientName("Buyer"); order.setRecipientPhone("0123456789"); order.setProvince("Province");
        order.setWard("Ward"); order.setDetailAddress("Address"); order = orderRepository.saveAndFlush(order);
        return new Fixture(payment, order, Math.abs(UUID.randomUUID().getMostSignificantBits()));
    }

    private AppUser user(String email, Role role) {
        AppUser user = new AppUser(); user.setFullName(role.name()); user.setEmail(email); user.setPassword("password");
        user.setPhone(email); user.setRole(role); user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    private SePayWebhookRequest request(long id, String code, String account, BigDecimal amount) {
        SePayWebhookRequest request = new SePayWebhookRequest(); request.setId(id); request.setGateway("Bank");
        request.setReferenceCode("REF-" + id); request.setTransferType("in"); request.setCode(code);
        request.setAccountNumber(account); request.setTransferAmount(amount); return request;
    }

    private SePayTransaction transaction(long id, String payload) {
        SePayTransaction transaction = new SePayTransaction(); transaction.setSePayTransactionId(id);
        transaction.setGateway("Bank"); transaction.setAccountNumber("123456");
        transaction.setTransferAmount(BigDecimal.ONE); transaction.setReferenceCode("REF-" + id);
        transaction.setRawPayload(payload); return transaction;
    }

    private record Fixture(Payment payment, Order order, long transactionId) {}
}
