package com.ecommerce.minishopee.service;


import com.ecommerce.minishopee.address.entity.Address;
import com.ecommerce.minishopee.category.enums.CategoryStatus;
import com.ecommerce.minishopee.dto.*;
import com.ecommerce.minishopee.entity.*;
import com.ecommerce.minishopee.exception.BadRequestException;
import com.ecommerce.minishopee.address.repository.AddressRepository;
import com.ecommerce.minishopee.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private static final BigDecimal SHIPPING_FEE_PER_SHOP = new BigDecimal("30000");
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;

    public CheckoutService(AddressRepository addressRepository, CartItemRepository cartItemRepository) {
        this.addressRepository = addressRepository;
        this.cartItemRepository = cartItemRepository;
    }

    /*
     * Checkout Preview:
     *
     * - Không tạo Order
     * - Không trừ tồn kho
     * - Không xóa CartItem
     */
    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(AppUser currentUser, CheckoutPreviewRequest request) {
        validateNoDuplicateCartItemIds(request.getCartItemIds());

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Địa chỉ nhận hàng không tồn tại hoặc không thuộc tài khoản hiện tại."));

        List<CartItem> selectedItems = cartItemRepository.findAllByIdInAndCart_User_Id(request.getCartItemIds(), currentUser.getId());

        /*
         * Nếu số lượng kết quả khác số lượng ID gửi lên, có CartItem không tồn tại hoặc thuộc user khác.
         */
        if (selectedItems.size() != request.getCartItemIds().size()) {
            throw new BadRequestException("Một hoặc nhiều CartItem không tồn tại hoặc không thuộc tài khoản hiện tại.");
        }

        selectedItems.forEach(this::validateCartItemForCheckout);

        Map<Long, List<CartItem>> itemsByShop = selectedItems.stream()
                .collect(Collectors.groupingBy(item ->
                                                                item.getProductVariant().getProduct().getShop().getId(),
                                                                LinkedHashMap::new,
                                                                Collectors.toList()
                ));

        List<CheckoutShopGroupResponse> shopGroups = new ArrayList<>();

        BigDecimal merchandiseTotal = BigDecimal.ZERO;

        int totalQuantity = 0;

        for (List<CartItem> shopItems : itemsByShop.values()) {
            CheckoutShopGroupResponse group = buildShopGroup(shopItems);

            shopGroups.add(group);

            merchandiseTotal = merchandiseTotal.add(group.getMerchandiseSubtotal());

            totalQuantity += group.getTotalQuantity();
        }

        BigDecimal shippingTotal = SHIPPING_FEE_PER_SHOP.multiply(BigDecimal.valueOf(shopGroups.size()));

        BigDecimal grandTotal = merchandiseTotal.add(shippingTotal);

        return CheckoutPreviewResponse.builder()
                .address(toAddressResponse(address))
                .shopGroups(shopGroups)
                .shopCount(shopGroups.size())
                .itemCount(selectedItems.size())
                .totalQuantity(totalQuantity)
                .merchandiseTotal(merchandiseTotal)
                .shippingTotal(shippingTotal)
                .grandTotal(grandTotal)
                .paymentMethod(PaymentMethod.COD)
                .build();
    }




    private CheckoutShopGroupResponse buildShopGroup(List<CartItem> shopItems) {
        CartItem firstItem = shopItems.getFirst();

        Shop shop = firstItem.getProductVariant().getProduct().getShop();

        List<CheckoutItemResponse> itemResponses = shopItems.stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal merchandiseSubtotal = itemResponses.stream()
                .map(CheckoutItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = itemResponses.stream()
                .mapToInt(CheckoutItemResponse::getQuantity)
                .sum();

        BigDecimal shopTotal = merchandiseSubtotal.add(SHIPPING_FEE_PER_SHOP);

        return CheckoutShopGroupResponse.builder()
                .shopId(shop.getId())
                .shopName(shop.getName())
                .items(itemResponses)
                .itemCount(itemResponses.size())
                .totalQuantity(totalQuantity)
                .merchandiseSubtotal(merchandiseSubtotal)
                .shippingFee(SHIPPING_FEE_PER_SHOP)
                .shopTotal(shopTotal)
                .build();
    }

    private CheckoutItemResponse toItemResponse(CartItem cartItem) {
        ProductVariant variant = cartItem.getProductVariant();

        Product product = variant.getProduct();

        BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return CheckoutItemResponse.builder()
                .cartItemId(cartItem.getId())
                .variantId(variant.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .variantName1(variant.getVariantName1())
                .variantValue1(variant.getVariantValue1())
                .variantName2(variant.getVariantName2())
                .variantValue2(variant.getVariantValue2())
                .unitPrice(variant.getPrice())
                .quantity(cartItem.getQuantity())
                .availableStock(variant.getStockQuantity())
                .lineTotal(lineTotal)
                .build();
    }

    private CheckoutAddressResponse toAddressResponse(Address address) {
        return CheckoutAddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .province(address.getProvince())
                .ward(address.getWard())
                .detailAddress(address.getDetailAddress())
                .note(address.getNote())
                .build();
    }

    /*
     * Preview phải kiểm tra lại trạng thái, không tin dữ liệu đã kiểm tra khi add Cart.
     */
    private void validateCartItemForCheckout(CartItem cartItem) {
        ProductVariant variant = cartItem.getProductVariant();

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

        Integer stock = variant.getStockQuantity();

        if (stock == null || stock <= 0) {
            throw new BadRequestException("SKU của sản phẩm \"" + product.getName() + "\" đã hết hàng."
            );
        }

        if (cartItem.getQuantity() > stock) {
            throw new BadRequestException("Số lượng sản phẩm \"" + product.getName() + "\" vượt quá tồn kho. Tồn kho hiện tại: " + stock);
        }

        if (product.getShop().getOwner().getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản người bán của sản phẩm \"" + product.getName() + "\" hiện đang bị khóa.");
        }
    }

    private void validateNoDuplicateCartItemIds(List<Long> cartItemIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>(cartItemIds);

        if (uniqueIds.size() != cartItemIds.size()) {
            throw new BadRequestException("Danh sách cartItemIds không được chứa ID trùng nhau.");
        }
    }
}
