package com.ecommerce.minishopee.order.service;

import com.ecommerce.minishopee.order.dto.request.CancelOrderRequest;
import com.ecommerce.minishopee.order.dto.response.OrderResponse;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.OrderCancelledBy;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.exception.OrderNotFoundException;
import com.ecommerce.minishopee.order.mapper.OrderMapper;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.product.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BuyerOrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderPaymentPolicy orderPaymentPolicy;

    public BuyerOrderService(
            OrderRepository orderRepository,
            ProductVariantRepository productVariantRepository,
            OrderPaymentPolicy orderPaymentPolicy
    ) {
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderPaymentPolicy = orderPaymentPolicy;
    }

    /*
     * Buyer xem danh sách đơn hàng của mình.
     *
     * status = null:   lấy tất cả.
     * status != null:  lọc theo trạng thái.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(AppUser currentUser, OrderStatus status) {
        List<Order> orders;

        if (status == null) {
            orders = orderRepository.findAllDetailedByBuyerId(currentUser.getId());
        } else {
            orders = orderRepository.findAllDetailedByBuyerIdAndStatus(currentUser.getId(), status);
        }

        return orders.stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    /*
     * Buyer xem chi tiết một đơn hàng.
     * Order phải thuộc currentUser.
     */
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(Long orderId, AppUser currentUser) {
        Order order = orderRepository.findDetailedByIdAndBuyerId(orderId, currentUser.getId())
                        .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderMapper.toResponse(order);
    }

    /*
     * Buyer hủy đơn.
     *
     * Chỉ hủy được:
     * - PENDING
     * - CONFIRMED
     *
     * Khi hủy:
     * - Hoàn tồn kho
     * - Chuyển status = CANCELLED
     * - Lưu thông tin người hủy
     */
    @Transactional
    public OrderResponse cancelMyOrder(Long orderId, AppUser currentUser, CancelOrderRequest request) {

        // Khóa Order trước để chống hủy hai lần.
        Order order = orderRepository.findOwnedOrderByIdForUpdate(orderId, currentUser.getId())
                        .orElseThrow(() -> new OrderNotFoundException(orderId));

        orderPaymentPolicy.validateCanCancelIndividually(order);
        validateBuyerCanCancel(order);

        /*
         * Một OrderItem thường tương ứng một SKU.
         *
         * Tuy nhiên vẫn gom quantity theo variantId để code an toàn nếu một Order có nhiều dòng cùng trỏ tới một SKU.
         */
        Map<Long, Integer> quantityByVariantId = new LinkedHashMap<>();

        for (OrderItem item : order.getItems()) {
            Long variantId = item.getProductVariant().getId();

            quantityByVariantId.merge(variantId, item.getQuantity(), Integer::sum);
        }

        // Khóa SKU theo thứ tự ID cố định để giảm nguy cơ deadlock.
        List<Long> sortedVariantIds = quantityByVariantId.keySet()
                                                            .stream()
                                                            .sorted()
                                                            .toList();

        List<ProductVariant> restoredVariants = new ArrayList<>();

        for (Long variantId : sortedVariantIds) {
            ProductVariant variant = productVariantRepository.findByIdForUpdate(variantId)
                            .orElseThrow(() -> new BadRequestException("Không tìm thấy SKU cần hoàn tồn kho: " + variantId));

            int returnedQuantity = quantityByVariantId.get(variantId);

            int currentStock = variant.getStockQuantity() == null
                                                            ? 0
                                                            : variant.getStockQuantity();

            int restoredStock = currentStock + returnedQuantity;

            variant.setStockQuantity(restoredStock);

            /*
             * Nếu SKU hết hàng do đơn này, sau khi hoàn stock thì kích hoạt lại.
             *
             * Không tự kích hoạt SKU đang INACTIVE hoặc DELETED vì Seller đã chủ động ẩn/xóa SKU đó.
             */
            if (variant.getStatus() == ProductVariantStatus.OUT_OF_STOCK && restoredStock > 0) {

                variant.setStatus(ProductVariantStatus.ACTIVE);
            }

            restoredVariants.add(variant);
        }

        productVariantRepository.saveAll(restoredVariants);

        order.setStatus(OrderStatus.CANCELLED);

        order.setCancelledBy(OrderCancelledBy.BUYER);

        order.setCancelReason(normalizeReason(request.getReason()));

        order.setCancelledAt(LocalDateTime.now());

        /*
         * Phase 1 chỉ dùng COD.
         *
         * Đơn PENDING/CONFIRMED vẫn đang UNPAID, nên không cần thay đổi paymentStatus.
         */
        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toResponse(savedOrder);
    }

    private void validateBuyerCanCancel(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng đã bị hủy trước đó.");
        }

        if (order.getStatus() == OrderStatus.SHIPPING) {
            throw new BadRequestException("Đơn hàng đang được giao nên không thể hủy.");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Đơn hàng đã hoàn thành nên không thể hủy.");
        }

        boolean cancellable =
                    order.getStatus() == OrderStatus.PENDING
                    || order.getStatus() == OrderStatus.CONFIRMED;

        if (!cancellable) {
            throw new BadRequestException("Không thể hủy đơn hàng ở trạng thái hiện tại.");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }

        return reason.trim();
    }
}
