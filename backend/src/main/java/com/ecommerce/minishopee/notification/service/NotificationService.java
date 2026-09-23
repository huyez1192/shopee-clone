package com.ecommerce.minishopee.notification.service;

import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.notification.dto.response.MarkAllReadResponse;
import com.ecommerce.minishopee.notification.dto.response.NotificationResponse;
import com.ecommerce.minishopee.notification.dto.response.UnreadCountResponse;
import com.ecommerce.minishopee.notification.entity.Notification;
import com.ecommerce.minishopee.notification.enums.NotificationType;
import com.ecommerce.minishopee.notification.exception.NotificationNotFoundException;
import com.ecommerce.minishopee.notification.repository.NotificationRepository;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.review.entity.Review;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyOrderCreated(Order order) {
        create(order.getBuyer(), NotificationType.ORDER_CREATED, "Đặt hàng thành công",
                "Đơn hàng " + order.getOrderCode() + " đã được tạo thành công.", buyerOrderUrl(order));
        create(order.getShop().getOwner(), NotificationType.NEW_ORDER, "Bạn có đơn hàng mới",
                "Shop nhận được đơn hàng mới " + order.getOrderCode() + ".", sellerOrderUrl(order));
    }

    @Transactional
    public void notifyOrderConfirmed(Order order) {
        create(order.getBuyer(), NotificationType.ORDER_CONFIRMED, "Đơn hàng đã được xác nhận",
                "Đơn hàng " + order.getOrderCode() + " đã được người bán xác nhận.", buyerOrderUrl(order));
    }

    @Transactional
    public void notifyOrderShipping(Order order) {
        create(order.getBuyer(), NotificationType.ORDER_SHIPPING, "Đơn hàng đang được giao",
                "Đơn hàng " + order.getOrderCode() + " đang trên đường giao đến bạn.", buyerOrderUrl(order));
    }

    @Transactional
    public void notifyOrderCompleted(Order order) {
        create(order.getBuyer(), NotificationType.ORDER_COMPLETED, "Đơn hàng đã hoàn thành",
                "Đơn hàng " + order.getOrderCode() + " đã hoàn thành.", buyerOrderUrl(order));
    }

    @Transactional
    public void notifyOrderCancelledToBuyer(Order order, boolean expired) {
        String message = expired
                ? "Đơn hàng " + order.getOrderCode() + " đã bị hủy do quá thời gian thanh toán."
                : "Đơn hàng " + order.getOrderCode() + " đã bị hủy.";
        create(order.getBuyer(), NotificationType.ORDER_CANCELLED, "Đơn hàng đã bị hủy", message,
                buyerOrderUrl(order));
    }

    @Transactional
    public void notifyOrderCancelledToSeller(Order order) {
        create(order.getShop().getOwner(), NotificationType.ORDER_CANCELLED, "Đơn hàng đã bị hủy",
                "Người mua đã hủy đơn hàng " + order.getOrderCode() + ".", sellerOrderUrl(order));
    }

    @Transactional
    public void notifyProductReviewed(Review review) {
        create(review.getProduct().getShop().getOwner(), NotificationType.PRODUCT_REVIEWED,
                "Sản phẩm có đánh giá mới",
                "Bạn nhận được đánh giá " + review.getRating() + " sao cho sản phẩm "
                        + review.getProduct().getName() + ".", "/seller/reviews");
    }

    @Transactional
    public void notifyShopApproved(Shop shop) {
        create(shop.getOwner(), NotificationType.SHOP_APPROVED, "Shop đã được phê duyệt",
                "Shop " + shop.getName() + " đã được phê duyệt.", "/seller");
    }

    @Transactional
    public void notifyShopRejected(Shop shop) {
        String reason = shop.getRejectionReason();
        String message = "Yêu cầu mở shop " + shop.getName() + " đã bị từ chối."
                + (reason == null || reason.isBlank() ? "" : " Lý do: " + reason);
        create(shop.getOwner(), NotificationType.SHOP_REJECTED, "Yêu cầu mở shop bị từ chối", message, "/seller");
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(
            AppUser currentUser, Boolean read, int page, int size
    ) {
        validatePage(page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationResponse> result = (read == null
                ? notificationRepository.findByRecipient_Id(currentUser.getId(), pageable)
                : notificationRepository.findByRecipient_IdAndRead(currentUser.getId(), read, pageable))
                .map(NotificationService::toResponse);
        return PageResponse.<NotificationResponse>builder()
                .content(result.getContent()).pageNumber(result.getNumber()).pageSize(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages())
                .last(result.isLast()).build();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(AppUser currentUser) {
        return new UnreadCountResponse(notificationRepository.countByRecipient_IdAndReadFalse(currentUser.getId()));
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, AppUser currentUser) {
        Notification notification = notificationRepository
                .findByIdAndRecipient_Id(notificationId, currentUser.getId())
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Transactional
    public MarkAllReadResponse markAllAsRead(AppUser currentUser) {
        return new MarkAllReadResponse(notificationRepository.markAllRead(currentUser.getId(), LocalDateTime.now()));
    }

    private void create(AppUser recipient, NotificationType type, String title, String message, String targetUrl) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetUrl(targetUrl);
        notificationRepository.save(notification);
    }

    private static NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder().id(notification.getId()).type(notification.getType())
                .title(notification.getTitle()).message(notification.getMessage())
                .targetUrl(notification.getTargetUrl()).read(notification.isRead())
                .readAt(notification.getReadAt()).createdAt(notification.getCreatedAt()).build();
    }

    private static String buyerOrderUrl(Order order) {
        return "/orders/" + order.getId();
    }

    private static String sellerOrderUrl(Order order) {
        return "/seller/orders/" + order.getId();
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("page phải >= 0 và size phải từ 1 đến 100.");
        }
    }
}
