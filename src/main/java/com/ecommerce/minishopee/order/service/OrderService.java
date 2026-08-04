package com.ecommerce.minishopee.order.service;

import com.ecommerce.minishopee.address.entity.Address;
import com.ecommerce.minishopee.cart.entity.CartItem;
import com.ecommerce.minishopee.category.enums.CategoryStatus;
import com.ecommerce.minishopee.order.dto.request.CreateOrderRequest;
import com.ecommerce.minishopee.order.dto.response.CreateOrderResponse;
import com.ecommerce.minishopee.order.dto.response.OrderResponse;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.order.enums.PaymentMethod;
import com.ecommerce.minishopee.order.enums.PaymentStatus;
import com.ecommerce.minishopee.product.exception.ProductVariantNotFoundException;
import com.ecommerce.minishopee.order.mapper.OrderMapper;
import com.ecommerce.minishopee.address.repository.AddressRepository;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductStatus;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.cart.repository.CartItemRepository;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.product.repository.ProductVariantRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    // Phải giống phí ship trong CheckoutService.
    private static final BigDecimal SHIPPING_FEE_PER_ORDER = new BigDecimal("30000");
    private static final DateTimeFormatter ORDER_CODE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;

    public OrderService(
            AddressRepository addressRepository,
            CartItemRepository cartItemRepository,
            ProductVariantRepository productVariantRepository,
            OrderRepository orderRepository
    ) {
        this.addressRepository = addressRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderRepository = orderRepository;
    }

    /*
     * Toàn bộ thao tác nằm trong một transaction.
     *
     * Chỉ cần một Order gặp lỗi:
     * - Không Order nào được tạo
     * - Không SKU nào bị trừ
     * - Không CartItem nào bị xóa
     */
    @Transactional
    public CreateOrderResponse createOrders(AppUser currentUser, CreateOrderRequest request) {
        validatePaymentMethod(request.getPaymentMethod());

        validateNoDuplicateCartItemIds(request.getCartItemIds());

        Address address =
                addressRepository
                        .findByIdAndUserId(request.getAddressId(), currentUser.getId())
                        .orElseThrow(() ->
                                new BadRequestException("Địa chỉ nhận hàng không tồn tại hoặc không thuộc tài khoản hiện tại.")
                        );

        /*
         * Sắp xếp ID để các transaction khóa
         * CartItem theo cùng một thứ tự.
         */
        List<Long> sortedCartItemIds = request.getCartItemIds()
                                                    .stream()
                                                    .sorted()
                                                    .toList();

        List<CartItem> selectedItems = cartItemRepository.findAllOwnedByIdsForUpdate(sortedCartItemIds, currentUser.getId());

        if (selectedItems.size() != sortedCartItemIds.size()) {
            throw new BadRequestException("Một hoặc nhiều CartItem không tồn tại hoặc không thuộc tài khoản hiện tại.");
        }

        // Khóa SKU theo thứ tự ID để hạn chế deadlock.
        List<Long> variantIds = selectedItems.stream()
                                            .map(item -> item.getProductVariant().getId())
                                            .distinct()
                                            .sorted()
                                            .toList();

        Map<Long, ProductVariant> lockedVariants = new LinkedHashMap<>();

        for (Long variantId : variantIds) {
            ProductVariant variant = productVariantRepository.findByIdForUpdate(variantId)
                            .orElseThrow(() -> new ProductVariantNotFoundException(variantId));

            lockedVariants.put(variantId, variant);
        }

        List<ValidatedOrderLine> lines = new ArrayList<>();

        for (CartItem cartItem : selectedItems) {
            Long variantId = cartItem.getProductVariant().getId();

            ProductVariant lockedVariant = lockedVariants.get(variantId);

            validateCartItemForOrder(cartItem, lockedVariant);

            lines.add(new ValidatedOrderLine(cartItem, lockedVariant));
        }

        // Mỗi Shop tạo một Order riêng.
        Map<Long, List<ValidatedOrderLine>> linesByShop =
                lines.stream()
                        .collect(
                                Collectors.groupingBy(
                                        line ->
                                                line.variant()
                                                        .getProduct()
                                                        .getShop()
                                                        .getId(),
                                        LinkedHashMap::new,
                                        Collectors.toList()
                                )
                        );

        List<Order> createdOrders = new ArrayList<>();

        for (List<ValidatedOrderLine> shopLines : linesByShop.values()) {
            Order order = createOrderForShop(currentUser, address, request.getPaymentMethod(), shopLines);

            Order savedOrder = orderRepository.save(order);

            createdOrders.add(savedOrder);
        }

        /*
         * Các ProductVariant đang được quản lý trong persistence context.
         *
         * saveAll giúp thể hiện rõ rằng tồn kho đã được cập nhật.
         */
        productVariantRepository.saveAll(lockedVariants.values());

        /*
         * Chỉ xóa CartItem được chọn.
         * Các CartItem không checkout vẫn giữ nguyên.
         */
        cartItemRepository.deleteAll(selectedItems);

        List<OrderResponse> orderResponses = createdOrders.stream()
                                                        .map(OrderMapper::toResponse)
                                                        .toList();

        BigDecimal merchandiseTotal = createdOrders.stream()
                                                    .map(Order::getMerchandiseSubtotal)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingTotal = createdOrders.stream()
                                                .map(Order::getShippingFee)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = createdOrders.stream()
                                            .map(Order::getTotalAmount)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CreateOrderResponse.builder()
                .orders(orderResponses)
                .orderCount(orderResponses.size())
                .merchandiseTotal(merchandiseTotal)
                .shippingTotal(shippingTotal)
                .grandTotal(grandTotal)
                .build();
    }

    private Order createOrderForShop(
            AppUser currentUser,
            Address address,
            PaymentMethod paymentMethod,
            List<ValidatedOrderLine> lines
    ) {
        Shop shop = lines.get(0)
                        .variant()
                        .getProduct()
                        .getShop();

        BigDecimal merchandiseSubtotal =
                lines.stream()
                        .map(line ->
                                line.variant()
                                        .getPrice()
                                        .multiply(
                                                BigDecimal.valueOf(
                                                        line.cartItem()
                                                                .getQuantity()
                                                )
                                        )
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalAmount = merchandiseSubtotal.add(SHIPPING_FEE_PER_ORDER);

        Order order = new Order();

        order.setOrderCode(generateUniqueOrderCode());
        order.setBuyer(currentUser);
        order.setShop(shop);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setMerchandiseSubtotal(merchandiseSubtotal);
        order.setShippingFee(SHIPPING_FEE_PER_ORDER);
        order.setTotalAmount(totalAmount);

        copyAddressSnapshot(address, order);

        for (ValidatedOrderLine line : lines) {
            CartItem cartItem = line.cartItem();

            ProductVariant variant = line.variant();

            Product product = variant.getProduct();

            int quantity = cartItem.getQuantity();

            BigDecimal unitPrice = variant.getPrice();

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            OrderItem orderItem = new OrderItem();

            orderItem.setProduct(product);

            orderItem.setProductVariant(variant);

            orderItem.setProductNameSnapshot(product.getName());

            orderItem.setProductImageUrlSnapshot(product.getImageUrl());

            orderItem.setSkuCodeSnapshot(variant.getSkuCode());

            orderItem.setVariantName1Snapshot(variant.getVariantName1());

            orderItem.setVariantValue1Snapshot(variant.getVariantValue1());

            orderItem.setVariantName2Snapshot(variant.getVariantName2());

            orderItem.setVariantValue2Snapshot(variant.getVariantValue2());

            orderItem.setUnitPrice(unitPrice);

            orderItem.setQuantity(quantity);

            orderItem.setLineTotal(lineTotal);

            order.addItem(orderItem);

            // Trừ tồn kho SKU.
            int remainingStock = variant.getStockQuantity() - quantity;

            variant.setStockQuantity(remainingStock);

            if (remainingStock == 0) {
                variant.setStatus(ProductVariantStatus.OUT_OF_STOCK);
            }
        }

        return order;
    }

    private void validateCartItemForOrder(CartItem cartItem, ProductVariant variant) {
        if (variant == null) {
            throw new BadRequestException("Không tìm thấy SKU của CartItem: " + cartItem.getId());
        }

        Product product = variant.getProduct();

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Sản phẩm \"" + product.getName() + "\" hiện không còn được bán.");
        }

        if (product.getShop().getStatus() != ShopStatus.APPROVED) {
            throw new BadRequestException("Shop của sản phẩm \"" + product.getName() + "\" hiện không hoạt động.");
        }

        if (product.getCategory().getStatus() != CategoryStatus.ACTIVE) {
            throw new BadRequestException("Danh mục của sản phẩm \"" + product.getName() + "\" hiện không hoạt động.");
        }

        if (variant.getStatus() != ProductVariantStatus.ACTIVE) {
            throw new BadRequestException("SKU của sản phẩm \"" + product.getName() + "\" hiện không còn được bán.");
        }

        Integer quantity = cartItem.getQuantity();

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Số lượng CartItem không hợp lệ.");
        }

        Integer stock = variant.getStockQuantity();

        if (stock == null || stock <= 0) {
            throw new BadRequestException("SKU của sản phẩm \"" + product.getName() + "\" đã hết hàng.");
        }

        if (quantity > stock) {
            throw new BadRequestException("Số lượng sản phẩm \"" + product.getName() + "\" vượt quá tồn kho. Tồn kho hiện tại: " + stock);
        }

        if (product.getShop().getOwner().getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản người bán của sản phẩm \"" + product.getName() + "\" hiện đang bị khóa.");
        }
    }

    private void copyAddressSnapshot(Address address, Order order) {
        order.setRecipientName(address.getRecipientName());

        order.setRecipientPhone(address.getRecipientPhone());

        order.setProvince(address.getProvince());

        order.setWard(address.getWard());

        order.setDetailAddress(address.getDetailAddress());

        order.setNote(address.getNote());
    }

    private void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod != PaymentMethod.COD) {
            throw new BadRequestException("Phase 1 hiện chỉ hỗ trợ thanh toán COD.");
        }
    }

    private void validateNoDuplicateCartItemIds(List<Long> cartItemIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>(cartItemIds);

        if (uniqueIds.size() != cartItemIds.size()) {
            throw new BadRequestException("Danh sách cartItemIds không được chứa ID trùng nhau.");
        }
    }

    private String generateUniqueOrderCode() {
        String orderCode;

        do {
            String timestamp = LocalDateTime.now().format(ORDER_CODE_TIME_FORMAT);

            String randomPart =
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 8)
                            .toUpperCase(Locale.ROOT);

            orderCode = "MS-" + timestamp + "-" + randomPart;

        } while (orderRepository.existsByOrderCode(orderCode));

        return orderCode;
    }

    private record ValidatedOrderLine(CartItem cartItem, ProductVariant variant) {
    }
}