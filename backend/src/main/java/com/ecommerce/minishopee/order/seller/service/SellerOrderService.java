package com.ecommerce.minishopee.order.seller.service;

import com.ecommerce.minishopee.order.dto.response.OrderResponse;
import com.ecommerce.minishopee.order.seller.dto.request.SellerCancelOrderRequest;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.OrderCancelledBy;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.order.enums.PaymentMethod;
import com.ecommerce.minishopee.order.enums.PaymentStatus;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.exception.OrderNotFoundException;
import com.ecommerce.minishopee.shop.exception.ShopNotFoundException;
import com.ecommerce.minishopee.order.mapper.OrderMapper;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.product.repository.ProductVariantRepository;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.minishopee.order.enums.PaymentMethod;
import com.ecommerce.minishopee.order.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SellerOrderService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final ProductVariantRepository productVariantRepository;

    public SellerOrderService(
            OrderRepository orderRepository,
            ShopRepository shopRepository,
            ProductVariantRepository productVariantRepository
    ) {
        this.orderRepository = orderRepository;
        this.shopRepository = shopRepository;
        this.productVariantRepository = productVariantRepository;
    }

    /*
     * Seller xem danh sách Order của shop mình.
     *
     * status = null:   lấy tất cả Order.
     *
     * status != null:  lọc theo trạng thái.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getShopOrders(AppUser currentUser, OrderStatus status) {
        Shop shop = getSellerShop(currentUser);

        List<Order> orders;

        if (status == null) {
            orders = orderRepository.findAllDetailedByShopId(shop.getId());
        } else {
            orders = orderRepository.findAllDetailedByShopIdAndStatus(shop.getId(), status);
        }

        return orders.stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    /*
     * Seller xem chi tiết Order.
     *
     * Order bắt buộc phải thuộc shop của Seller.
     */
    @Transactional(readOnly = true)
    public OrderResponse getShopOrderById(Long orderId, AppUser currentUser) {
        Shop shop = getSellerShop(currentUser);

        Order order = orderRepository.findDetailedByIdAndShopId(orderId, shop.getId())
                                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderMapper.toResponse(order);
    }

    // PENDING → CONFIRMED
    @Transactional
    public OrderResponse confirmOrder(Long orderId, AppUser currentUser) {

        Shop shop = getApprovedShop(currentUser);

        Order order = findSellerOrderForUpdate(orderId, shop.getId());

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể xác nhận đơn hàng đang ở trạng thái PENDING.");
        }

        validateOrderCanBeConfirmed(order);

        order.setStatus(OrderStatus.CONFIRMED);

        order.setConfirmedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toResponse(savedOrder);
    }

    // CONFIRMED → SHIPPING
    @Transactional
    public OrderResponse shipOrder(Long orderId, AppUser currentUser) {
        Shop shop = getApprovedShop(currentUser);

        Order order = findSellerOrderForUpdate(orderId, shop.getId());

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Chỉ có thể giao đơn hàng đang ở trạng thái CONFIRMED.");
        }

        order.setStatus(OrderStatus.SHIPPING);

        order.setShippedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toResponse(savedOrder);
    }

    /*
     * SHIPPING → COMPLETED
     *
     * Với COD: paymentStatus chuyển sang PAID.
     */
    @Transactional
    public OrderResponse completeOrder(Long orderId, AppUser currentUser) {

        Shop shop = getApprovedShop(currentUser);

        Order order = findSellerOrderForUpdate(orderId, shop.getId());

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new BadRequestException("Chỉ có thể hoàn thành đơn hàng đang ở trạng thái SHIPPING.");
        }

        order.setStatus(OrderStatus.COMPLETED);

        order.setCompletedAt(LocalDateTime.now());

        // COD chỉ được coi là đã thanh toán khi giao hàng hoàn tất.
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toResponse(savedOrder);
    }

    /*
     * Seller hủy Order.
     *
     * Chỉ hủy được:
     * - PENDING
     * - CONFIRMED
     *
     * Khi hủy phải hoàn lại tồn kho.
     */
    @Transactional
    public OrderResponse cancelOrder(
            Long orderId,
            AppUser currentUser,
            SellerCancelOrderRequest request
    ) {
        Shop shop = getApprovedShop(currentUser);

        Order order = findSellerOrderForUpdate(orderId, shop.getId());

        validateSellerCanCancel(order);

        restoreOrderStock(order);

        order.setStatus(OrderStatus.CANCELLED);

        order.setCancelledBy(OrderCancelledBy.SELLER);

        order.setCancelReason(request.getReason().trim());

        order.setCancelledAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toResponse(savedOrder);
    }

    /*
     * Seller được xem Order ngay cả khi Shop LOCKED.
     *
     * Nhưng không được cập nhật trạng thái khi Shop không còn APPROVED.
     */
    private Shop getSellerShop(AppUser currentUser) {
        return shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ShopNotFoundException("Bạn chưa có shop."));
    }

    private Shop getApprovedShop(AppUser currentUser) {

        Shop shop = getSellerShop(currentUser);

        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new BadRequestException("Shop chưa được duyệt hoặc hiện không hoạt động.");
        }

        return shop;
    }

    private Order findSellerOrderForUpdate(Long orderId, Long shopId) {
        return orderRepository.findSellerOrderByIdForUpdate(orderId, shopId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void validateSellerCanCancel(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng đã bị hủy trước đó.");
        }

        if (order.getStatus() == OrderStatus.SHIPPING) {
            throw new BadRequestException("Đơn hàng đang được giao nên không thể hủy.");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Đơn hàng đã hoàn thành nên không thể hủy.");
        }

        boolean cancellable = order.getStatus() == OrderStatus.PENDING
                        || order.getStatus() == OrderStatus.CONFIRMED;

        if (!cancellable) {
            throw new BadRequestException("Không thể hủy đơn hàng ở trạng thái hiện tại.");
        }
    }

    // Hoàn lại tồn kho khi Seller hủy Order.
    private void restoreOrderStock(Order order) {
        /*
         * Gom quantity theo variantId.
         *
         * Cách này vẫn đúng nếu một Order có nhiều OrderItem cùng SKU.
         */
        Map<Long, Integer> quantityByVariantId = new LinkedHashMap<>();

        for (OrderItem item : order.getItems()) {
            Long variantId = item.getProductVariant().getId();

            quantityByVariantId.merge(variantId, item.getQuantity(), Integer::sum);
        }

        List<Long> sortedVariantIds = quantityByVariantId.keySet()
                                                        .stream()
                                                        .sorted()
                                                        .toList();

        List<ProductVariant> restoredVariants = new ArrayList<>();

        for (Long variantId : sortedVariantIds) {
            ProductVariant variant = productVariantRepository.findByIdForUpdate(variantId)
                            .orElseThrow(() -> new BadRequestException("Không tìm thấy SKU cần hoàn tồn kho: " + variantId));

            int returnedQuantity = quantityByVariantId.get(variantId);

            int currentStock = variant.getStockQuantity()
                                                        == null
                                                        ? 0
                                                        : variant.getStockQuantity();

            int restoredStock = currentStock + returnedQuantity;

            variant.setStockQuantity(restoredStock);

            /*
             * Nếu SKU đang OUT_OF_STOCK và được hoàn hàng thì chuyển lại ACTIVE.
             *
             * Không tự kích hoạt SKU INACTIVE hoặc DELETED.
             */
            if (variant.getStatus() == ProductVariantStatus.OUT_OF_STOCK && restoredStock > 0) {

                variant.setStatus(ProductVariantStatus.ACTIVE);
            }

            restoredVariants.add(variant);
        }

        productVariantRepository.saveAll(restoredVariants);
    }

    private void validateOrderCanBeConfirmed(
            Order order
    ) {
        if (
                order.getPaymentMethod()
                        == PaymentMethod.SEPAY
                        && order.getPaymentStatus()
                        != PaymentStatus.PAID
        ) {
            throw new BadRequestException(
                    "Đơn hàng SePay chưa được thanh toán."
            );
        }
    }
}