package com.ecommerce.minishopee.statistics.repository;

import com.ecommerce.minishopee.category.entity.Category;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.*;
import com.ecommerce.minishopee.order.repository.OrderItemRepository;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductStatus;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.review.entity.Review;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.statistics.service.AdminStatisticsService;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatisticsRepositoryRegressionTests {
    @Autowired EntityManager entityManager;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired AdminStatisticsService adminStatisticsService;

    private AppUser buyer;
    private Shop shopA;
    private Shop shopB;
    private Product productA;
    private Product productB;
    private ProductVariant variantA;
    private ProductVariant variantB;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        buyer = user("buyer-" + suffix, Role.USER);
        shopA = shop(user("seller-a-" + suffix, Role.SELLER), "A-" + suffix);
        shopB = shop(user("seller-b-" + suffix, Role.SELLER), "B-" + suffix);
        Category category = new Category(); category.setName("Category-" + suffix); category.setSlug("category-" + suffix);
        entityManager.persist(category);
        productA = product(shopA, category, "Product A"); productB = product(shopB, category, "Product B");
        variantA = variant(productA, "SKU-A-" + suffix); variantB = variant(productB, "SKU-B-" + suffix);
        entityManager.flush();
    }

    @Test
    void statusDistributionAndTotalCreatedUseCreatedAtHalfOpenRange() {
        LocalDateTime from = LocalDateTime.of(2040, 1, 1, 0, 0);
        LocalDateTime toExclusive = LocalDateTime.of(2040, 1, 3, 0, 0);
        Order atFrom = order("AT-FROM", shopA, OrderStatus.PENDING, PaymentMethod.COD, PaymentStatus.UNPAID, null);
        Order beforeEnd = order("BEFORE-END", shopA, OrderStatus.CANCELLED, PaymentMethod.COD, PaymentStatus.UNPAID, null);
        Order atEnd = order("AT-END", shopA, OrderStatus.CONFIRMED, PaymentMethod.COD, PaymentStatus.UNPAID, null);
        Order completedInsideButCreatedOutside = order("OUTSIDE", shopA, OrderStatus.COMPLETED,
                PaymentMethod.COD, PaymentStatus.PAID, LocalDateTime.of(2040, 1, 2, 12, 0));
        entityManager.flush();
        atFrom.setCreatedAt(from); beforeEnd.setCreatedAt(toExclusive.minusSeconds(1));
        atEnd.setCreatedAt(toExclusive); completedInsideButCreatedOutside.setCreatedAt(from.minusDays(1));
        entityManager.flush(); entityManager.clear();

        assertEquals(2, orderRepository.countCreatedOrders(shopA.getId(), from, toExclusive));
        Map<OrderStatus, Long> counts = orderRepository.countByCurrentStatus(shopA.getId(), from, toExclusive)
                .stream().collect(Collectors.toMap(row -> row.getStatus(), row -> row.getOrderCount()));
        assertEquals(1L, counts.get(OrderStatus.PENDING));
        assertEquals(1L, counts.get(OrderStatus.CANCELLED));
        assertEquals(null, counts.get(OrderStatus.COMPLETED));
    }

    @Test
    void cancelledIsExcludedWhileCompletedPaidCodAndSePayAreBothIncluded() {
        LocalDateTime from = LocalDateTime.of(2041, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2041, 2, 1, 0, 0);
        Order cod = order("COD", shopA, OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID,
                LocalDateTime.of(2041, 1, 10, 10, 0));
        Order sePay = order("SEPAY", shopA, OrderStatus.COMPLETED, PaymentMethod.SEPAY, PaymentStatus.PAID,
                LocalDateTime.of(2041, 1, 11, 10, 0));
        Order cancelled = order("CANCELLED", shopA, OrderStatus.CANCELLED, PaymentMethod.COD, PaymentStatus.PAID,
                LocalDateTime.of(2041, 1, 12, 10, 0));
        money(cod, "100", "10"); money(sePay, "150", "10"); money(cancelled, "9900", "100");
        item(cod, productA, variantA, 2, new BigDecimal("100"));
        item(sePay, productA, variantA, 3, new BigDecimal("150"));
        item(cancelled, productA, variantA, 99, new BigDecimal("9900"));
        entityManager.flush();

        var summary = orderRepository.summarizeCompletedOrders(shopA.getId(), from, to);
        assertMoney("250", summary.getCompletedRevenue());
        assertMoney("270", summary.getCompletedGmv());
        assertEquals(2L, summary.getCompletedOrderCount());
        assertEquals(5, orderItemRepository.sumUnitsSold(shopA.getId(), from, to));
        var top = orderItemRepository.findTopProducts(shopA.getId(), from, to, PageRequest.of(0, 5));
        assertEquals(5L, top.getFirst().getUnitsSold());
        assertMoney("250", top.getFirst().getMerchandiseValue());
    }

    @Test
    void dayAndMonthQueriesExecuteAndGroupCompletedSnapshotsCorrectly() {
        Order januaryA = order("JAN-A", shopA, OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID,
                LocalDateTime.of(2042, 1, 10, 9, 0));
        Order januaryB = order("JAN-B", shopA, OrderStatus.COMPLETED, PaymentMethod.SEPAY, PaymentStatus.PAID,
                LocalDateTime.of(2042, 1, 11, 9, 0));
        Order february = order("FEB", shopA, OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID,
                LocalDateTime.of(2042, 2, 2, 9, 0));
        money(januaryA, "40", "4"); money(januaryB, "60", "6"); money(february, "90", "9");
        item(januaryA, productA, variantA, 1, new BigDecimal("40"));
        item(januaryB, productA, variantA, 2, new BigDecimal("60"));
        item(february, productA, variantA, 3, new BigDecimal("90"));
        entityManager.flush();
        LocalDateTime from = LocalDateTime.of(2042, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2042, 3, 1, 0, 0);

        var days = orderRepository.summarizeCompletedOrdersByDay(shopA.getId(), from, to);
        var months = orderRepository.summarizeCompletedOrdersByMonth(shopA.getId(), from, to);
        var unitMonths = orderItemRepository.sumUnitsSoldByMonth(shopA.getId(), from, to);
        assertEquals(3, days.size());
        assertEquals(2, months.size());
        assertMoney("100", months.getFirst().getCompletedRevenue());
        assertEquals(2L, months.getFirst().getCompletedOrderCount());
        assertEquals(3L, unitMonths.getFirst().getUnitsSold());
        assertMoney("90", months.getLast().getCompletedRevenue());
    }

    @Test
    void shopRatingIsAllTimeAndIsolatedFromOtherShop() {
        Order orderA1 = order("REVIEW-A1", shopA, OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID, LocalDateTime.now());
        Order orderA2 = order("REVIEW-A2", shopA, OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID, LocalDateTime.now());
        Order orderA3 = order("REVIEW-A3", shopA, OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID, LocalDateTime.now());
        Order orderB = order("REVIEW-B", shopB, OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID, LocalDateTime.now());
        review(item(orderA1, productA, variantA, 1, BigDecimal.TEN), productA, 5);
        review(item(orderA2, productA, variantA, 1, BigDecimal.TEN), productA, 5);
        review(item(orderA3, productA, variantA, 1, BigDecimal.TEN), productA, 3);
        review(item(orderB, productB, variantB, 1, BigDecimal.TEN), productB, 1);
        entityManager.flush();

        var summary = reviewRepository.summarizeShopRatings(shopA.getId());
        assertEquals(3L, summary.getReviewCount());
        assertEquals(13.0 / 3.0, summary.getAverageRating(), 0.0001);
        assertEquals(2L, summary.getFiveStarCount());
        assertEquals(1L, summary.getThreeStarCount());
        assertEquals(0L, summary.getOneStarCount());
    }

    @Test
    void adminOverviewSeparatesLifetimeCountsAndCompletedRangeMetrics() {
        long usersBefore = count("AppUser"); long shopsBefore = count("Shop");
        long productsBefore = count("Product"); long ordersBefore = orderRepository.count();
        Order completed = order("ADMIN-COMPLETED", shopA, OrderStatus.COMPLETED, PaymentMethod.COD,
                PaymentStatus.PAID, LocalDateTime.of(2043, 5, 10, 10, 0));
        Order cancelled = order("ADMIN-CANCELLED", shopA, OrderStatus.CANCELLED, PaymentMethod.COD,
                PaymentStatus.PAID, LocalDateTime.of(2043, 5, 11, 10, 0));
        money(completed, "400", "20"); money(cancelled, "5000", "100");
        item(completed, productA, variantA, 4, new BigDecimal("400"));
        item(cancelled, productA, variantA, 50, new BigDecimal("5000"));
        completed.setCreatedAt(LocalDateTime.of(2043, 5, 10, 8, 0));
        cancelled.setCreatedAt(LocalDateTime.of(2043, 5, 11, 8, 0));
        entityManager.flush();
        var response = adminStatisticsService.getOverview(LocalDate.of(2043, 5, 1), LocalDate.of(2043, 5, 31));

        assertEquals(usersBefore, response.lifetimeTotalUsers());
        assertEquals(shopsBefore, response.lifetimeTotalShops());
        assertEquals(productsBefore, response.lifetimeTotalProducts());
        assertEquals(ordersBefore + 2, response.lifetimeTotalOrders());
        assertEquals(1, response.completedOrders());
        assertMoney("420", response.completedGmv());
        assertMoney("400", response.completedMerchandiseValue());
        assertEquals(4, response.unitsSold());
        Map<OrderStatus, Long> statuses = response.orderStatusDistribution().stream()
                .collect(Collectors.toMap(item -> item.status(), item -> item.count()));
        assertEquals(1L, statuses.get(OrderStatus.COMPLETED));
        assertEquals(1L, statuses.get(OrderStatus.CANCELLED));
    }

    private long count(String entity) { return entityManager.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult(); }
    private AppUser user(String value, Role role) { AppUser u = new AppUser(); u.setFullName(value); u.setEmail(value + "@test.local"); u.setPassword("password"); u.setPhone(value); u.setRole(role); u.setStatus(UserStatus.ACTIVE); entityManager.persist(u); return u; }
    private Shop shop(AppUser owner, String name) { Shop s = new Shop(); s.setOwner(owner); s.setName(name); s.setAddress("Address"); s.setContactPhone("0123456789"); s.setStatus(ShopStatus.APPROVED); entityManager.persist(s); return s; }
    private Product product(Shop shop, Category category, String name) { Product p = new Product(); p.setShop(shop); p.setCategory(category); p.setName(name); p.setStatus(ProductStatus.ACTIVE); entityManager.persist(p); return p; }
    private ProductVariant variant(Product product, String sku) { ProductVariant v = new ProductVariant(); v.setProduct(product); v.setSkuCode(sku); v.setPrice(new BigDecimal("9999")); v.setStockQuantity(100); v.setStatus(ProductVariantStatus.ACTIVE); entityManager.persist(v); return v; }
    private Order order(String suffix, Shop shop, OrderStatus status, PaymentMethod method, PaymentStatus payment, LocalDateTime completedAt) { Order o = new Order(); o.setOrderCode("REG-" + suffix.substring(0, Math.min(suffix.length(), 18)) + "-" + UUID.randomUUID().toString().substring(0, 8)); o.setBuyer(buyer); o.setShop(shop); o.setStatus(status); o.setPaymentMethod(method); o.setPaymentStatus(payment); o.setMerchandiseSubtotal(new BigDecimal("100")); o.setShippingFee(new BigDecimal("10")); o.setTotalAmount(new BigDecimal("110")); o.setRecipientName("Buyer"); o.setRecipientPhone("0123456789"); o.setProvince("Province"); o.setWard("Ward"); o.setDetailAddress("Address"); o.setCompletedAt(completedAt); entityManager.persist(o); return o; }
    private OrderItem item(Order order, Product product, ProductVariant variant, int quantity, BigDecimal total) { OrderItem i = new OrderItem(); i.setOrder(order); i.setProduct(product); i.setProductVariant(variant); i.setProductNameSnapshot(product.getName()); i.setUnitPrice(total.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP)); i.setQuantity(quantity); i.setLineTotal(total); entityManager.persist(i); return i; }
    private void money(Order order, String merchandise, String shipping) { order.setMerchandiseSubtotal(new BigDecimal(merchandise)); order.setShippingFee(new BigDecimal(shipping)); order.setTotalAmount(new BigDecimal(merchandise).add(new BigDecimal(shipping))); }
    private void review(OrderItem item, Product product, int rating) { Review r = new Review(); r.setBuyer(buyer); r.setProduct(product); r.setOrderItem(item); r.setRating(rating); entityManager.persist(r); }
    private void assertMoney(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
}
