package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.address.entity.Address;
import com.ecommerce.minishopee.cart.entity.Cart;
import com.ecommerce.minishopee.cart.entity.CartItem;
import com.ecommerce.minishopee.category.entity.Category;
import com.ecommerce.minishopee.category.enums.CategoryStatus;
import com.ecommerce.minishopee.order.dto.request.CreateOrderRequest;
import com.ecommerce.minishopee.order.dto.response.CreateOrderResponse;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.PaymentMethod;
import com.ecommerce.minishopee.order.enums.PaymentStatus;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.order.service.OrderService;
import com.ecommerce.minishopee.payment.dto.request.SePayWebhookRequest;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductStatus;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SePayMultiShopIntegrationTests {
    @Autowired EntityManager entityManager;
    @Autowired OrderService orderService;
    @Autowired SePayWebhookProcessor webhookProcessor;
    @Autowired PaymentRepository paymentRepository;
    @Autowired OrderRepository orderRepository;

    @Test
    void multiShopCheckoutCreatesOnePaymentAndWebhookPaysEveryLinkedOrder() {
        long initialPaymentCount = paymentRepository.count();
        AppUser buyer = user("multishop-buyer@example.test", Role.USER);
        AppUser sellerA = user("multishop-a@example.test", Role.SELLER);
        AppUser sellerB = user("multishop-b@example.test", Role.SELLER);
        Category category = category();
        ProductVariant variantA = variant(shop(sellerA, "Multi Shop A"), category, "Product A", "SKU-MULTI-A", 100_000);
        ProductVariant variantB = variant(shop(sellerB, "Multi Shop B"), category, "Product B", "SKU-MULTI-B", 200_000);
        Address address = address(buyer);
        Cart cart = new Cart(); cart.setUser(buyer); entityManager.persist(cart);
        CartItem itemA = cartItem(cart, variantA, 1);
        CartItem itemB = cartItem(cart, variantB, 2);
        entityManager.flush();

        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddressId(address.getId());
        request.setCartItemIds(List.of(itemA.getId(), itemB.getId()));
        request.setPaymentMethod(PaymentMethod.SEPAY);

        CreateOrderResponse response = orderService.createOrders(buyer, request);
        entityManager.flush();
        assertEquals(2, response.getOrders().size());
        assertEquals(initialPaymentCount + 1, paymentRepository.count());

        Payment payment = paymentRepository.findByPaymentCode(response.getPayment().getPaymentCode()).orElseThrow();
        List<Order> orders = orderRepository.findAllByPaymentIdForUpdateOrdered(payment.getId());
        assertEquals(2, orders.size());
        assertEquals(orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add), payment.getAmount());

        SePayWebhookRequest webhook = new SePayWebhookRequest();
        webhook.setId(88001L); webhook.setGateway("TEST_BANK"); webhook.setTransferType("in");
        webhook.setCode(payment.getPaymentCode()); webhook.setAccountNumber(payment.getAccountNumber());
        webhook.setTransferAmount(payment.getAmount()); webhook.setReferenceCode("REF-88001");
        webhookProcessor.processFirstDelivery(webhook, "{\"id\":88001}");
        entityManager.flush();

        assertEquals(PaymentSessionStatus.PAID, paymentRepository.findById(payment.getId()).orElseThrow().getStatus());
        orderRepository.findAllByPaymentIdForUpdateOrdered(payment.getId())
                .forEach(order -> assertEquals(PaymentStatus.PAID, order.getPaymentStatus()));
    }

    private AppUser user(String email, Role role) {
        AppUser user = new AppUser(); user.setFullName(email); user.setEmail(email); user.setPassword("password");
        user.setPhone(email); user.setRole(role); user.setStatus(UserStatus.ACTIVE); entityManager.persist(user); return user;
    }

    private Shop shop(AppUser owner, String name) {
        Shop shop = new Shop(); shop.setOwner(owner); shop.setName(name); shop.setAddress("Address");
        shop.setContactPhone("0123456789"); shop.setStatus(ShopStatus.APPROVED); entityManager.persist(shop); return shop;
    }

    private Category category() {
        Category category = new Category(); category.setName("Multi Shop Category");
        category.setSlug("multi-shop-category"); category.setStatus(CategoryStatus.ACTIVE);
        entityManager.persist(category); return category;
    }

    private ProductVariant variant(Shop shop, Category category, String name, String sku, long price) {
        Product product = new Product(); product.setName(name); product.setShop(shop); product.setCategory(category);
        product.setStatus(ProductStatus.ACTIVE); entityManager.persist(product);
        ProductVariant variant = new ProductVariant(); variant.setProduct(product); variant.setSkuCode(sku);
        variant.setPrice(BigDecimal.valueOf(price)); variant.setStockQuantity(10);
        variant.setStatus(ProductVariantStatus.ACTIVE); entityManager.persist(variant); return variant;
    }

    private Address address(AppUser buyer) {
        Address address = new Address(); address.setUser(buyer); address.setRecipientName("Buyer");
        address.setRecipientPhone("0123456789"); address.setProvince("HCM"); address.setWard("Ward");
        address.setDetailAddress("Detail"); entityManager.persist(address); return address;
    }

    private CartItem cartItem(Cart cart, ProductVariant variant, int quantity) {
        CartItem item = new CartItem(); item.setCart(cart); item.setProductVariant(variant);
        item.setQuantity(quantity); entityManager.persist(item); return item;
    }
}
