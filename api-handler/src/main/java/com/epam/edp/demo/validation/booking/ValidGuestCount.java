package com.epam.edp.demo.validation.booking;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GuestCountValidator.class)
public @interface ValidGuestCount {
    String message() default "max 10 guests are allowed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
