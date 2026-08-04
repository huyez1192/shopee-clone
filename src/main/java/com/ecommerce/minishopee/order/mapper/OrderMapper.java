package com.ecommerce.minishopee.order.mapper;

import com.ecommerce.minishopee.order.dto.response.OrderAddressResponse;
import com.ecommerce.minishopee.order.dto.response.OrderItemResponse;
import com.ecommerce.minishopee.order.dto.response.OrderResponse;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.entity.OrderItem;

import java.util.List;

public class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems()
                                                .stream()
                                                .map(OrderMapper::toItemResponse)
                                                .toList();

        int totalQuantity = items.stream()
                                    .mapToInt(OrderItemResponse::getQuantity)
                                    .sum();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .buyerId(order.getBuyer().getId())
                .shopId(order.getShop().getId())
                .shopName(order.getShop().getName())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .address(OrderAddressResponse.builder()
                                .recipientName(order.getRecipientName())
                                .recipientPhone(order.getRecipientPhone())
                                .province(order.getProvince())
                                .ward(order.getWard())
                                .detailAddress(order.getDetailAddress())
                                .note(order.getNote())
                                .build()
                )
                .items(items)
                .itemCount(items.size())
                .totalQuantity(totalQuantity)
                .merchandiseSubtotal(order.getMerchandiseSubtotal())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .cancelledBy(order.getCancelledBy())
                .cancelReason(order.getCancelReason())
                .cancelledAt(order.getCancelledAt())
                .confirmedAt(order.getConfirmedAt())
                .shippedAt(order.getShippedAt())
                .completedAt(order.getCompletedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .variantId(item.getProductVariant().getId())

                 // Các thông tin hiển thị dùng snapshot, không lấy tên/ảnh hiện tại của Product.
                .productName(item.getProductNameSnapshot())
                .productImageUrl(item.getProductImageUrlSnapshot())
                .skuCode(item.getSkuCodeSnapshot())
                .variantName1(item.getVariantName1Snapshot())
                .variantValue1(item.getVariantValue1Snapshot())
                .variantName2(item.getVariantName2Snapshot())
                .variantValue2(item.getVariantValue2Snapshot())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineTotal(item.getLineTotal())
                .build();
    }
}