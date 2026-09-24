package com.ecommerce.minishopee.common.exception;

import com.ecommerce.minishopee.address.exception.AddressNotFoundException;
import com.ecommerce.minishopee.cart.exception.CartItemNotFoundException;
import com.ecommerce.minishopee.category.exception.CategoryNotFoundException;
import com.ecommerce.minishopee.order.exception.OrderNotFoundException;
import com.ecommerce.minishopee.notification.exception.NotificationNotFoundException;
import com.ecommerce.minishopee.product.exception.ProductNotFoundException;
import com.ecommerce.minishopee.product.exception.ProductVariantNotFoundException;
import com.ecommerce.minishopee.review.exception.OrderItemNotFoundException;
import com.ecommerce.minishopee.review.exception.ReviewAccessDeniedException;
import com.ecommerce.minishopee.review.exception.ReviewConflictException;
import com.ecommerce.minishopee.review.exception.ReviewNotFoundException;
import com.ecommerce.minishopee.shop.exception.ShopNotFoundException;
import com.ecommerce.minishopee.user.exception.UserNotFoundException;
import com.ecommerce.minishopee.upload.exception.ImageUploadException;
import com.ecommerce.minishopee.upload.exception.InvalidImageException;
import com.ecommerce.minishopee.upload.exception.ImageUploadUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({InvalidImageException.class, MaxUploadSizeExceededException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidImage(RuntimeException exception, HttpServletRequest request) {
        String message = exception instanceof MaxUploadSizeExceededException
                ? "Tệp ảnh không được vượt quá 5 MB."
                : exception.getMessage();
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ImageUploadException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, Object> handleImageUpload(ImageUploadException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(ImageUploadUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handleImageUploadUnavailable(
            ImageUploadUnavailableException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Tham số yêu cầu không hợp lệ hoặc bị thiếu.", request);
    }

    @ExceptionHandler({ReviewNotFoundException.class, OrderItemNotFoundException.class,
            NotificationNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleReviewNotFound(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ReviewConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleReviewConflict(ReviewConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(ReviewAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleReviewAccessDenied(
            ReviewAccessDeniedException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    private Map<String, Object> error(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("path", request.getRequestURI());
        return error;
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleProductNotFoundException(
            ProductNotFoundException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", exception.getMessage());
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequestException(
            BadRequestException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", exception.getMessage());
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", "Validation failed");
        error.put("errors", errors);
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleCategoryNotFoundException(
            CategoryNotFoundException exception,
            HttpServletRequest request) {

        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", exception.getMessage());
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(AddressNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleAddressNotFoundException(
            AddressNotFoundException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", exception.getMessage());
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(ShopNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleShopNotFoundException(
            ShopNotFoundException exception,
            HttpServletRequest request) {

        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", exception.getMessage());
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(ProductVariantNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleProductVariantNotFoundException(
            ProductVariantNotFoundException exception,
            HttpServletRequest request
    ) {

        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", exception.getMessage());
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleCartItemNotFoundException(
            CartItemNotFoundException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", exception.getMessage());
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleOrderNotFoundException(
            OrderNotFoundException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", exception.getMessage());
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleUserNotFoundException(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", exception.getMessage());
        error.put("path",
                request.getRequestURI()
        );

        return error;
    }

    @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleLockedException(
            org.springframework.security.authentication.LockedException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.UNAUTHORIZED.value());
        error.put("error", "Unauthorized");
        error.put("message", "Tài khoản đã bị khóa.");
        error.put("path", request.getRequestURI());

        return error;
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request
    ) {
        return Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", "Email hoặc mật khẩu không chính xác.",
                "path", request.getRequestURI()
        );
    }
}
