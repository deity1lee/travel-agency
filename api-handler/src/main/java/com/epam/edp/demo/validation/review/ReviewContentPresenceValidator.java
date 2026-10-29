package com.epam.edp.demo.validation.review;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.epam.edp.demo.dto.review.CreateReviewRequestDTO;

public class ReviewContentPresenceValidator implements ConstraintValidator<ValidReviewContentPresence, CreateReviewRequestDTO> {

    private String message;

    @Override
    public void initialize(ValidReviewContentPresence annotation) {
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(CreateReviewRequestDTO request, ConstraintValidatorContext context) {
        if (request.rate() == null) {
            return true;
        }

        if (request.rate() <= 3 && (request.reviewContent() == null || request.reviewContent().isBlank())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("reviewContent")
                    .addConstraintViolation();

            return false;
        }

        return true;
    }
}
