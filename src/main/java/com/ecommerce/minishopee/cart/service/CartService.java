package com.ecommerce.minishopee.cart.service;

import com.ecommerce.minishopee.cart.entity.Cart;
import com.ecommerce.minishopee.cart.entity.CartItem;
import com.ecommerce.minishopee.cart.enums.CartItemAvailability;
import com.ecommerce.minishopee.category.enums.CategoryStatus;
import com.ecommerce.minishopee.cart.dto.request.AddCartItemRequest;
import com.ecommerce.minishopee.cart.dto.response.CartItemResponse;
import com.ecommerce.minishopee.cart.dto.response.CartResponse;
import com.ecommerce.minishopee.cart.dto.request.UpdateCartItemRequest;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.cart.exception.CartItemNotFoundException;
import com.ecommerce.minishopee.product.exception.ProductVariantNotFoundException;
import com.ecommerce.minishopee.cart.mapper.CartMapper;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductStatus;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.cart.repository.CartItemRepository;
import com.ecommerce.minishopee.cart.repository.CartRepository;
import com.ecommerce.minishopee.product.repository.ProductVariantRepository;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductVariantRepository productVariantRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
    }

    // Xem Cart hiện tại.
    @Transactional
    public CartResponse getCart(AppUser currentUser) {
        Cart cart = getOrCreateCart(currentUser);

        return buildCartResponse(cart);
    }

    // Thêm SKU vào Cart.
    // Nếu cùng SKU đã tồn tại: cộng thêm quantity.
    @Transactional
    public CartResponse addItem(AppUser currentUser, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(currentUser);

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(request.getVariantId()));

        CartItem existingItem = cartItemRepository.findByCart_IdAndProductVariant_Id(cart.getId(), variant.getId())
                .orElse(null);

        int newQuantity = request.getQuantity();

        if (existingItem != null) {
            newQuantity += existingItem.getQuantity();
        }

        validateVariantForCart(variant, newQuantity);

        if (existingItem == null) {
            CartItem cartItem = new CartItem();

            cartItem.setCart(cart);
            cartItem.setProductVariant(variant);
            cartItem.setQuantity(request.getQuantity());

            cartItemRepository.save(cartItem);
        } else {
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        }

        return buildCartResponse(cart);
    }

    // Cập nhật quantity thành giá trị mới.
    @Transactional
    public CartResponse updateItem(Long cartItemId, AppUser currentUser, UpdateCartItemRequest request) {
        CartItem cartItem = findOwnerCartItem(cartItemId, currentUser.getId());
        ProductVariant variant = cartItem.getProductVariant();
        validateVariantForCart(variant, request.getQuantity());
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return buildCartResponse(cartItem.getCart());
    }


    // Xóa một CartItem.
    @Transactional
    public void deleteItem(Long cartItemId, AppUser currentUser) {
        CartItem cartItem = findOwnerCartItem(cartItemId, currentUser.getId());
        cartItemRepository.delete(cartItem);
    }

    // Xóa toàn bộ Cart.
    @Transactional
    public void clearCart(AppUser currentUser) {
        cartRepository.findByUserId(currentUser.getId())
                .ifPresent(cart -> cartItemRepository.deleteAllByCart_Id(cart.getId()));
    }





    // Tạo Cart tự động nếu user chưa có
    // Nếu có cart rồi thì lấy cart đó
    private Cart getOrCreateCart(AppUser currentUser) {
        return cartRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(currentUser);
                    return cartRepository.save(cart);
                });
    }


    // Kiểm tra điều kiện khi add/update Cart.
    private void validateVariantForCart(ProductVariant variant, int requestQuantity) {
        Product product = variant.getProduct();

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Sản phẩm hiện không còn được bán.");
        }

        if (product.getShop().getStatus() != ShopStatus.APPROVED) {
            throw new BadRequestException("Shop hiện không hoạt động.");
        }

        if (product.getCategory().getStatus() != CategoryStatus.ACTIVE) {
            throw new BadRequestException("Danh mục sản phẩm hiện không hoạt động.");
        }

        if (variant.getStatus() == ProductVariantStatus.OUT_OF_STOCK) {
            throw new BadRequestException("SKU đã hết hàng.");
        }

        if (variant.getStatus() != ProductVariantStatus.ACTIVE) {
            throw new BadRequestException("SKU hiện không còn được bán.");
        }

        Integer stock = variant.getStockQuantity();

        if (stock == null || stock <= 0) {
            throw new BadRequestException("SKU đã hết hàng");
        }

        if (requestQuantity > stock) {
            throw new BadRequestException("Số lượng yêu cầu vượt quá tồn kho. Tồn kho hiện tại: " + stock);
        }

        if (product.getShop().getOwner().getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản người bán hiện đang bị khóa.");
        }
    }

    // Ownership: CartItem phải thuộc Cart của currentUser.
    private CartItem findOwnerCartItem (Long cartItemId, Long userId) {
        return cartItemRepository.findByIdAndCart_User_Id(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));
    }

    // Chuyển Cart thành response.
    //
    // Khi xem Cart, không throw nếu item cũ đã hết hàng hoặc bị ẩn.
    // Thay vào đó trả available = false.
    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findAllByCart_IdOrderByCreatedAtAsc(cart.getId());

        List<CartItemResponse> responses = items.stream()
                .map(item -> {
                    AvailabilityResult result = checkAvailability(item);

                    return CartMapper.toItemResponse(item, result.status, result.reason());
                })
                .toList();

        int totalQuantity = responses.stream()
                .mapToInt(CartItemResponse :: getQuantity)
                .sum();

        BigDecimal totalAmount = responses.stream()
                .filter(CartItemResponse::isAvailable)
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUser().getId())
                .items(responses)
                .itemCount(responses.size())
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();
    }

    // Kiểm tra trạng thái hiện tại của item khi hiển thị Cart.
    private AvailabilityResult checkAvailability(CartItem item) {
        ProductVariant variant = item.getProductVariant();

        Product product = variant.getProduct();

        if (product.getStatus() != ProductStatus.ACTIVE) {
            return new AvailabilityResult(CartItemAvailability.PRODUCT_UNAVAILABLE, "Sản phẩm hiện không còn được bán.");
        }

        if (product.getCategory().getStatus() != CategoryStatus.ACTIVE) {
            return new AvailabilityResult(CartItemAvailability.CATEGORY_UNAVAILABLE, "Danh mục sản phẩm hiện không hoạt động.");
        }

        if (product.getShop().getStatus() != ShopStatus.APPROVED) {
            return new AvailabilityResult(CartItemAvailability.SHOP_UNAVAILABLE, "Shop hiện không hoạt động.");
        }

        if (variant.getStatus() == ProductVariantStatus.OUT_OF_STOCK
            || variant.getStockQuantity() == null
            || variant.getStockQuantity() <= 0) {
            return new AvailabilityResult(CartItemAvailability.OUT_OF_STOCK, "SKU đã hết hàng.");
        }

        if (variant.getStatus() != ProductVariantStatus.ACTIVE) {
            return new AvailabilityResult(CartItemAvailability.VARIANT_INACTIVE, "SKU hiện không còn được bán.");
        }

        if (item.getQuantity() > variant.getStockQuantity()) {
            return new AvailabilityResult(CartItemAvailability.INSUFFICIENT_STOCK, "Số lượng trong giỏ vượt quá tồn kho hiện tại.");
        }

        if (product.getShop().getOwner().getStatus() != UserStatus.ACTIVE) {
            return new AvailabilityResult(CartItemAvailability.SELLER_ACCOUNT_LOCKED, "Tài khoản người bán hiện đang bị khóa.");
        }

        return new AvailabilityResult(CartItemAvailability.AVAILABLE, null);
    }

    private record AvailabilityResult(CartItemAvailability status, String reason) {
    }
}
