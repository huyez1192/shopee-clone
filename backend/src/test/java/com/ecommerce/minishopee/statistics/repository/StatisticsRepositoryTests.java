package com.ecommerce.minishopee.statistics.repository;

import com.ecommerce.minishopee.category.entity.Category;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.*;
import com.ecommerce.minishopee.order.repository.OrderItemRepository;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentProvider;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatisticsRepositoryTests {
    @Autowired EntityManager entityManager;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;

    @Test
    void aggregatesOrderSnapshotsWithoutPaymentDoubleCountAndRespectsShopAndRange() {
        AppUser buyer = user("buyer@test.local", Role.USER);
        Shop shopA = shop(user("seller-a@test.local", Role.SELLER), "Shop A");
        Shop shopB = shop(user("seller-b@test.local", Role.SELLER), "Shop B");
        Category category = new Category(); category.setName("Category"); category.setSlug("category");
        entityManager.persist(category);
        Product productA = product(shopA, category, "Current A");
        Product productB = product(shopB, category, "Current B");
        ProductVariant variantA = variant(productA, "A-SKU", new BigDecimal("9999"));
        ProductVariant variantB = variant(productB, "B-SKU", new BigDecimal("8888"));

        Payment payment = new Payment(); payment.setPaymentCode("PAY-MULTI"); payment.setUser(buyer);
        payment.setProvider(PaymentProvider.SEPAY); payment.setAmount(new BigDecimal("999999"));
        payment.setStatus(PaymentSessionStatus.PAID); entityManager.persist(payment);

        LocalDateTime inside = LocalDateTime.of(2026, 9, 10, 12, 0);
        Order orderA = order(buyer, shopA, "A", OrderStatus.COMPLETED, PaymentStatus.PAID,
                new BigDecimal("100"), new BigDecimal("15"), inside);
        Order orderB = order(buyer, shopB, "B", OrderStatus.COMPLETED, PaymentStatus.PAID,
                new BigDecimal("200"), new BigDecimal("20"), inside);
        payment.addOrder(orderA); payment.addOrder(orderB);
        entityManager.persist(orderA); entityManager.persist(orderB);
        item(orderA, productA, variantA, "Snapshot A", 2, new BigDecimal("100"));
        item(orderB, productB, variantB, "Snapshot B", 3, new BigDecimal("200"));

        Order pendingPaid = order(buyer, shopA, "PENDING", OrderStatus.PENDING, PaymentStatus.PAID,
                new BigDecimal("500"), BigDecimal.ZERO, inside);
        entityManager.persist(pendingPaid); item(pendingPaid, productA, variantA, "Ignored", 10, new BigDecimal("500"));
        Order unpaidCompleted = order(buyer, shopA, "UNPAID", OrderStatus.COMPLETED, PaymentStatus.UNPAID,
                new BigDecimal("600"), BigDecimal.ZERO, inside);
        entityManager.persist(unpaidCompleted); item(unpaidCompleted, productA, variantA, "Ignored", 10, new BigDecimal("600"));
        Order boundary = order(buyer, shopA, "BOUNDARY", OrderStatus.COMPLETED, PaymentStatus.PAID,
                new BigDecimal("700"), BigDecimal.ZERO, LocalDateTime.of(2026, 10, 1, 0, 0));
        entityManager.persist(boundary); item(boundary, productA, variantA, "Ignored", 10, new BigDecimal("700"));
        entityManager.flush();

        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime toExclusive = LocalDateTime.of(2026, 10, 1, 0, 0);
        var sellerA = orderRepository.summarizeCompletedOrders(shopA.getId(), from, toExclusive);
        var sellerB = orderRepository.summarizeCompletedOrders(shopB.getId(), from, toExclusive);
        var admin = orderRepository.summarizeCompletedOrders(null, from, toExclusive);

        assertEquals(0, new BigDecimal("100").compareTo(sellerA.getCompletedRevenue()));
        assertEquals(0, new BigDecimal("115").compareTo(sellerA.getCompletedGmv()));
        assertEquals(1L, sellerA.getCompletedOrderCount());
        assertEquals(2L, orderItemRepository.sumUnitsSold(shopA.getId(), from, toExclusive));
        assertEquals(0, new BigDecimal("200").compareTo(sellerB.getCompletedRevenue()));
        assertEquals(0, new BigDecimal("335").compareTo(admin.getCompletedGmv()));
        assertEquals(2L, admin.getCompletedOrderCount());

        var top = orderItemRepository.findTopProducts(shopA.getId(), from, toExclusive, PageRequest.of(0, 5));
        assertEquals("Snapshot A", top.getFirst().getProductName());
        assertEquals(2L, top.getFirst().getUnitsSold());
        assertEquals(0, new BigDecimal("100").compareTo(top.getFirst().getMerchandiseValue()));
    }

    private AppUser user(String email, Role role) {
        AppUser user = new AppUser(); user.setFullName(email); user.setEmail(email); user.setPassword("password");
        user.setPhone(email); user.setRole(role); user.setStatus(UserStatus.ACTIVE); entityManager.persist(user); return user;
    }

    private Shop shop(AppUser owner, String name) {
        Shop shop = new Shop(); shop.setOwner(owner); shop.setName(name); shop.setAddress("Address");
        shop.setContactPhone("0123456789"); shop.setStatus(ShopStatus.APPROVED); entityManager.persist(shop); return shop;
    }

    private Product product(Shop shop, Category category, String name) {
        Product product = new Product(); product.setShop(shop); product.setCategory(category); product.setName(name);
        product.setStatus(ProductStatus.ACTIVE); entityManager.persist(product); return product;
    }

    private ProductVariant variant(Product product, String sku, BigDecimal currentPrice) {
        ProductVariant variant = new ProductVariant(); variant.setProduct(product); variant.setSkuCode(sku);
        variant.setPrice(currentPrice); variant.setStockQuantity(10); variant.setStatus(ProductVariantStatus.ACTIVE);
        entityManager.persist(variant); return variant;
    }

    private Order order(AppUser buyer, Shop shop, String code, OrderStatus status, PaymentStatus paymentStatus,
                        BigDecimal merchandise, BigDecimal shipping, LocalDateTime completedAt) {
        Order order = new Order(); order.setOrderCode("STAT-" + code); order.setBuyer(buyer); order.setShop(shop);
        order.setStatus(status); order.setPaymentStatus(paymentStatus); order.setPaymentMethod(PaymentMethod.SEPAY);
        order.setMerchandiseSubtotal(merchandise); order.setShippingFee(shipping);
        order.setTotalAmount(merchandise.add(shipping)); order.setRecipientName("Buyer");
        order.setRecipientPhone("0123456789"); order.setProvince("Province"); order.setWard("Ward");
        order.setDetailAddress("Address"); order.setCompletedAt(completedAt); return order;
    }

    private void item(Order order, Product product, ProductVariant variant, String snapshotName,
                      int quantity, BigDecimal lineTotal) {
        OrderItem item = new OrderItem(); item.setOrder(order); item.setProduct(product); item.setProductVariant(variant);
        item.setProductNameSnapshot(snapshotName); item.setProductImageUrlSnapshot("https://example.com/image.jpg");
        item.setUnitPrice(lineTotal.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP)); item.setQuantity(quantity);
        item.setLineTotal(lineTotal); entityManager.persist(item);
    }
}
