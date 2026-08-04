package com.ecommerce.minishopee.service;

import com.ecommerce.minishopee.dto.AdminCancelOrderRequest;
import com.ecommerce.minishopee.dto.AdminUpdateOrderStatusRequest;
import com.ecommerce.minishopee.dto.OrderResponse;
import com.ecommerce.minishopee.entity.Order;
import com.ecommerce.minishopee.entity.OrderCancelledBy;
import com.ecommerce.minishopee.entity.OrderItem;
import com.ecommerce.minishopee.entity.OrderStatus;
import com.ecommerce.minishopee.entity.PaymentMethod;
import com.ecommerce.minishopee.entity.PaymentStatus;
import com.ecommerce.minishopee.entity.ProductVariant;
import com.ecommerce.minishopee.entity.ProductVariantStatus;
import com.ecommerce.minishopee.exception.BadRequestException;
import com.ecommerce.minishopee.exception.OrderNotFoundException;
import com.ecommerce.minishopee.mapper.OrderMapper;
import com.ecommerce.minishopee.repository.OrderRepository;
import com.ecommerce.minishopee.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;

    public AdminOrderService(
        OrderRepository orderRepository,
        ProductVariantRepository productVariantRepository
    ) {
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
    }

    // Admin xem, tìm kiếm và lọc toàn bộ Order.
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(
            String keyword,
            OrderStatus status,
            Long buyerId,
            Long shopId
    ) {
        String normalizedKeyword = keyword == null || keyword.isBlank()
                                    ? ""
                                    : keyword.trim().toLowerCase();

        List<Order> orders = orderRepository.searchDetailedForAdmin(
                                                                    normalizedKeyword,
                                                                    status,
                                                                    buyerId,
                                                                    shopId
                            );

        return orders.stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    // Admin xem chi tiết bất kỳ Order nào.
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findDetailedByIdForAdmin(orderId)
                        .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderMapper.toResponse(order);
    }

    /*
     * Admin cập nhật trạng thái Order.
     *
     * Luồng hợp lệ:
     * PENDING → CONFIRMED
     * CONFIRMED → SHIPPING
     * SHIPPING → COMPLETED
     */
    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            AdminUpdateOrderStatusRequest request
    ) {
        Order order = findOrderForUpdate(orderId);

        OrderStatus targetStatus = request.getStatus();

        validateAndApplyStatusTransition(order, targetStatus);

        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toResponse(savedOrder);
    }

    /*
     * Admin hủy bất kỳ Order nào trong phạm vi trạng thái được phép.
     *
     * Chỉ hủy được:
     * PENDING
     * CONFIRMED
     */
    @Transactional
    public OrderResponse cancelOrder(
            Long orderId,
            AdminCancelOrderRequest request
    ) {
        Order order = findOrderForUpdate(orderId);

        validateAdminCanCancel(order);

        restoreOrderStock(order);

        order.setStatus(OrderStatus.CANCELLED);

        order.setCancelledBy(OrderCancelledBy.ADMIN);

        order.setCancelReason(normalize(request.getReason()));

        order.setCancelledAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toResponse(savedOrder);
    }

    // Kiểm tra và thực hiện đúng một bước chuyển trạng thái.
    private void validateAndApplyStatusTransition(
            Order order,
            OrderStatus targetStatus
    ) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Không thể cập nhật đơn hàng đã bị hủy.");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Đơn hàng đã hoàn thành.");
        }

        if (targetStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("Hãy sử dụng API hủy đơn hàng.");
        }

        if (targetStatus == OrderStatus.PENDING) {
            throw new BadRequestException("Không thể chuyển đơn hàng trở lại PENDING.");
        }

        switch (targetStatus) {

            case CONFIRMED -> confirmOrder(order);

            case SHIPPING -> shipOrder(order);

            case COMPLETED -> completeOrder(order);

            default -> throw new BadRequestException("Trạng thái đơn hàng không hợp lệ.");
        }
    }

    // PENDING → CONFIRMED
    private void confirmOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể chuyển đơn PENDING sang CONFIRMED.");
        }

        order.setStatus(OrderStatus.CONFIRMED);

        order.setConfirmedAt(LocalDateTime.now());
    }

    /*
     * CONFIRMED → SHIPPING
     */
    private void shipOrder(Order order) {
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Chỉ có thể chuyển đơn CONFIRMED sang SHIPPING.");
        }

        order.setStatus(OrderStatus.SHIPPING);

        order.setShippedAt(LocalDateTime.now());
    }

    // SHIPPING → COMPLETED
    private void completeOrder(Order order) {
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new BadRequestException("Chỉ có thể chuyển đơn SHIPPING sang COMPLETED.");
        }

        order.setStatus(OrderStatus.COMPLETED);

        order.setCompletedAt(LocalDateTime.now());

        // Với COD, đơn hoàn thành nghĩa là người mua đã thanh toán.
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
    }

    // Kiểm tra điều kiện Admin hủy Order.
    private void validateAdminCanCancel(Order order) {
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
            throw new BadRequestException("Không thể hủy đơn ở trạng thái hiện tại.");
        }
    }

    // Hoàn tồn kho khi Admin hủy Order.

    private void restoreOrderStock(Order order) {
        Map<Long, Integer> quantityByVariantId = new LinkedHashMap<>();

        // Gom tổng quantity theo SKU.
        for (OrderItem item : order.getItems()) {

            Long variantId = item.getProductVariant().getId();

            quantityByVariantId.merge(
                    variantId,
                    item.getQuantity(),
                    Integer::sum
            );
        }

        // Khóa SKU theo một thứ tự cố định để giảm nguy cơ deadlock.
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
             * Chỉ tự kích hoạt lại SKU đang OUT_OF_STOCK.
             *
             * Không kích hoạt SKU seller đã chủ động INACTIVE hoặc DELETED.
             */
            if (variant.getStatus() == ProductVariantStatus.OUT_OF_STOCK && restoredStock > 0) {

                variant.setStatus(ProductVariantStatus.ACTIVE);
            }

            restoredVariants.add(variant);
        }

        productVariantRepository.saveAll(restoredVariants);
    }

    private Order findOrderForUpdate(Long orderId) {
        return orderRepository.findAdminOrderByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}