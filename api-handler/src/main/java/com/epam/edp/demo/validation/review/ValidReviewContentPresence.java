package com.epam.edp.demo.validation.review;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReviewContentPresenceValidator.class)
public @interface ValidReviewContentPresence {
    String message() default "Review is required when rate is 1-3";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
