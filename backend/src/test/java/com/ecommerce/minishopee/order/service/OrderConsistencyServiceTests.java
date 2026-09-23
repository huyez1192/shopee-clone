package com.ecommerce.minishopee.order.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.admin.dto.request.AdminCancelOrderRequest;
import com.ecommerce.minishopee.order.admin.dto.request.AdminUpdateOrderStatusRequest;
import com.ecommerce.minishopee.order.admin.service.AdminOrderService;
import com.ecommerce.minishopee.order.dto.request.CancelOrderRequest;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.order.enums.PaymentMethod;
import com.ecommerce.minishopee.order.enums.PaymentStatus;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.order.seller.dto.request.SellerCancelOrderRequest;
import com.ecommerce.minishopee.order.seller.service.SellerOrderService;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.product.repository.ProductVariantRepository;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.notification.service.NotificationService;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderConsistencyServiceTests {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private ShopRepository shopRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private NotificationService notificationService;

    private final OrderPaymentPolicy paymentPolicy = new OrderPaymentPolicy();
    private BuyerOrderService buyerService;
    private SellerOrderService sellerService;
    private AdminOrderService adminService;
    private AppUser buyer;
    private Shop shop;

    @BeforeEach
    void setUp() {
        buyerService = new BuyerOrderService(orderRepository, variantRepository, paymentPolicy, reviewRepository, notificationService);
        sellerService = new SellerOrderService(orderRepository, shopRepository, variantRepository, paymentPolicy, notificationService);
        adminService = new AdminOrderService(orderRepository, variantRepository, paymentPolicy, notificationService);

        buyer = new AppUser();
        buyer.setId(10L);
        buyer.setFullName("Buyer");

        shop = new Shop();
        shop.setId(20L);
        shop.setName("Shop");
        shop.setStatus(ShopStatus.APPROVED);

        lenient().when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sellerRejectsUnpaidSePayButAllowsPaidSePayAndCodConfirmation() {
        Order unpaid = order(PaymentMethod.SEPAY, PaymentStatus.UNPAID, OrderStatus.PENDING);
        prepareSeller(unpaid);

        assertThrows(BadRequestException.class, () -> sellerService.confirmOrder(unpaid.getId(), buyer));
        assertEquals(OrderStatus.PENDING, unpaid.getStatus());

        Order paid = order(PaymentMethod.SEPAY, PaymentStatus.PAID, OrderStatus.PENDING);
        prepareSeller(paid);
        sellerService.confirmOrder(paid.getId(), buyer);
        assertEquals(OrderStatus.CONFIRMED, paid.getStatus());

        Order cod = order(PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.PENDING);
        prepareSeller(cod);
        sellerService.confirmOrder(cod.getId(), buyer);
        assertEquals(OrderStatus.CONFIRMED, cod.getStatus());
        verify(notificationService, times(2)).notifyOrderConfirmed(any(Order.class));
    }

    @Test
    void adminRejectsUnpaidSePayFulfillment() {
        Order order = order(PaymentMethod.SEPAY, PaymentStatus.UNPAID, OrderStatus.PENDING);
        prepareAdmin(order);

        assertThrows(BadRequestException.class,
                () -> adminService.updateOrderStatus(order.getId(), statusRequest(OrderStatus.CONFIRMED)));
        assertEquals(OrderStatus.PENDING, order.getStatus());
        verifyNoInteractions(notificationService);
    }

    @Test
    void allActorsRejectUnpaidSePayIndividualCancellationWithoutRestoringStock() {
        Order buyerOrder = orderWithStock(PaymentMethod.SEPAY, PaymentStatus.UNPAID, OrderStatus.PENDING, 4);
        prepareBuyer(buyerOrder);
        assertThrows(BadRequestException.class,
                () -> buyerService.cancelMyOrder(buyerOrder.getId(), buyer, new CancelOrderRequest()));
        assertUnchangedAfterRejectedCancellation(buyerOrder, 4);

        Order sellerOrder = orderWithStock(PaymentMethod.SEPAY, PaymentStatus.UNPAID, OrderStatus.PENDING, 5);
        prepareSeller(sellerOrder);
        assertThrows(BadRequestException.class,
                () -> sellerService.cancelOrder(sellerOrder.getId(), buyer, sellerCancelRequest()));
        assertUnchangedAfterRejectedCancellation(sellerOrder, 5);

        Order adminOrder = orderWithStock(PaymentMethod.SEPAY, PaymentStatus.UNPAID, OrderStatus.PENDING, 6);
        prepareAdmin(adminOrder);
        assertThrows(BadRequestException.class,
                () -> adminService.cancelOrder(adminOrder.getId(), new AdminCancelOrderRequest()));
        assertUnchangedAfterRejectedCancellation(adminOrder, 6);

        verifyNoInteractions(variantRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void allActorsRejectPaidSePayCancellationWithoutChangingPaymentOrderOrStock() {
        Payment payment = new Payment();
        payment.setStatus(PaymentSessionStatus.PAID);

        Order buyerOrder = orderWithStock(PaymentMethod.SEPAY, PaymentStatus.PAID, OrderStatus.PENDING, 7);
        buyerOrder.setPayment(payment);
        prepareBuyer(buyerOrder);
        assertThrows(BadRequestException.class,
                () -> buyerService.cancelMyOrder(buyerOrder.getId(), buyer, new CancelOrderRequest()));

        Order sellerOrder = orderWithStock(PaymentMethod.SEPAY, PaymentStatus.PAID, OrderStatus.CONFIRMED, 8);
        sellerOrder.setPayment(payment);
        prepareSeller(sellerOrder);
        assertThrows(BadRequestException.class,
                () -> sellerService.cancelOrder(sellerOrder.getId(), buyer, sellerCancelRequest()));

        Order adminOrder = orderWithStock(PaymentMethod.SEPAY, PaymentStatus.PAID, OrderStatus.CONFIRMED, 9);
        adminOrder.setPayment(payment);
        prepareAdmin(adminOrder);
        assertThrows(BadRequestException.class,
                () -> adminService.cancelOrder(adminOrder.getId(), new AdminCancelOrderRequest()));

        assertEquals(PaymentSessionStatus.PAID, payment.getStatus());
        assertUnchangedAfterRejectedCancellation(buyerOrder, 7);
        assertUnchangedAfterRejectedCancellation(sellerOrder, 8);
        assertUnchangedAfterRejectedCancellation(adminOrder, 9);
        verifyNoInteractions(variantRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void codPendingAndConfirmedCancellationStillRestoreStockOnce() {
        Order buyerOrder = orderWithStock(PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.PENDING, 3);
        prepareBuyer(buyerOrder);
        ProductVariant buyerVariant = buyerOrder.getItems().getFirst().getProductVariant();
        when(variantRepository.findByIdForUpdate(buyerVariant.getId())).thenReturn(Optional.of(buyerVariant));

        buyerService.cancelMyOrder(buyerOrder.getId(), buyer, new CancelOrderRequest());
        assertEquals(OrderStatus.CANCELLED, buyerOrder.getStatus());
        assertEquals(5, buyerVariant.getStockQuantity());
        assertNotNull(buyerOrder.getCancelledAt());

        Order sellerOrder = orderWithStock(PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.CONFIRMED, 4);
        prepareSeller(sellerOrder);
        ProductVariant sellerVariant = sellerOrder.getItems().getFirst().getProductVariant();
        when(variantRepository.findByIdForUpdate(sellerVariant.getId())).thenReturn(Optional.of(sellerVariant));

        sellerService.cancelOrder(sellerOrder.getId(), buyer, sellerCancelRequest());
        assertEquals(OrderStatus.CANCELLED, sellerOrder.getStatus());
        assertEquals(6, sellerVariant.getStockQuantity());
        assertNotNull(sellerOrder.getCancelledAt());
        verify(notificationService).notifyOrderCancelledToSeller(buyerOrder);
        verify(notificationService).notifyOrderCancelledToBuyer(sellerOrder, false);
    }

    @Test
    void invalidAdminTransitionsAndCompletedCancellationAreRejected() {
        Order pending = order(PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.PENDING);
        prepareAdmin(pending);
        assertThrows(BadRequestException.class,
                () -> adminService.updateOrderStatus(pending.getId(), statusRequest(OrderStatus.SHIPPING)));

        Order confirmed = order(PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.CONFIRMED);
        prepareAdmin(confirmed);
        assertThrows(BadRequestException.class,
                () -> adminService.updateOrderStatus(confirmed.getId(), statusRequest(OrderStatus.COMPLETED)));

        Order completed = order(PaymentMethod.COD, PaymentStatus.PAID, OrderStatus.COMPLETED);
        prepareAdmin(completed);
        assertThrows(BadRequestException.class,
                () -> adminService.cancelOrder(completed.getId(), new AdminCancelOrderRequest()));
    }

    @Test
    void validCodFlowCompletesAndMarksPaymentPaid() {
        Order order = order(PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.PENDING);
        prepareSeller(order);

        sellerService.confirmOrder(order.getId(), buyer);
        sellerService.shipOrder(order.getId(), buyer);
        sellerService.completeOrder(order.getId(), buyer);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        verify(notificationService).notifyOrderConfirmed(order);
        verify(notificationService).notifyOrderShipping(order);
        verify(notificationService).notifyOrderCompleted(order);
    }

    @Test
    void paidSePayCompletesWithoutChangingOnlinePaymentState() {
        Payment payment = new Payment();
        payment.setStatus(PaymentSessionStatus.PAID);
        Order order = order(PaymentMethod.SEPAY, PaymentStatus.PAID, OrderStatus.SHIPPING);
        order.setPayment(payment);
        prepareSeller(order);

        sellerService.completeOrder(order.getId(), buyer);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(PaymentSessionStatus.PAID, payment.getStatus());
    }

    private void prepareBuyer(Order order) {
        when(orderRepository.findOwnedOrderByIdForUpdate(order.getId(), buyer.getId()))
                .thenReturn(Optional.of(order));
    }

    private void prepareSeller(Order order) {
        when(shopRepository.findByOwnerId(buyer.getId())).thenReturn(Optional.of(shop));
        when(orderRepository.findSellerOrderByIdForUpdate(order.getId(), shop.getId()))
                .thenReturn(Optional.of(order));
    }

    private void prepareAdmin(Order order) {
        when(orderRepository.findAdminOrderByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    }

    private Order order(PaymentMethod method, PaymentStatus paymentStatus, OrderStatus status) {
        Order order = new Order();
        order.setId(System.nanoTime());
        order.setOrderCode("ORDER-" + order.getId());
        order.setBuyer(buyer);
        order.setShop(shop);
        order.setPaymentMethod(method);
        order.setPaymentStatus(paymentStatus);
        order.setStatus(status);
        order.setMerchandiseSubtotal(BigDecimal.TEN);
        order.setShippingFee(BigDecimal.ONE);
        order.setTotalAmount(BigDecimal.valueOf(11));
        return order;
    }

    private Order orderWithStock(
            PaymentMethod method,
            PaymentStatus paymentStatus,
            OrderStatus status,
            int stock
    ) {
        Order order = order(method, paymentStatus, status);
        Product product = new Product();
        product.setId(30L);
        product.setName("Product");
        ProductVariant variant = new ProductVariant();
        variant.setId(order.getId() + 1);
        variant.setProduct(product);
        variant.setStockQuantity(stock);
        variant.setStatus(ProductVariantStatus.ACTIVE);
        OrderItem item = new OrderItem();
        item.setId(40L);
        item.setProduct(product);
        item.setProductVariant(variant);
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.ONE);
        item.setLineTotal(BigDecimal.valueOf(2));
        order.addItem(item);
        return order;
    }

    private AdminUpdateOrderStatusRequest statusRequest(OrderStatus status) {
        AdminUpdateOrderStatusRequest request = new AdminUpdateOrderStatusRequest();
        request.setStatus(status);
        return request;
    }

    private SellerCancelOrderRequest sellerCancelRequest() {
        SellerCancelOrderRequest request = new SellerCancelOrderRequest();
        request.setReason("Seller reason");
        return request;
    }

    private void assertUnchangedAfterRejectedCancellation(Order order, int stock) {
        assertNotEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(stock, order.getItems().getFirst().getProductVariant().getStockQuantity());
        assertNull(order.getCancelledAt());
    }
}
