package com.ecommerce.minishopee.review.service;

import com.ecommerce.minishopee.review.dto.request.CreateReviewRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ReviewValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void ratingZeroFailsValidation() {
        assertFalse(validator.validate(request(0)).isEmpty());
    }

    @Test
    void ratingSixFailsValidation() {
        assertFalse(validator.validate(request(6)).isEmpty());
    }

    private CreateReviewRequest request(int rating) {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setOrderItemId(1L);
        request.setRating(rating);
        return request;
    }
}
