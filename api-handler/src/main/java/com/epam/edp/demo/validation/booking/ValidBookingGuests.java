package com.epam.edp.demo.validation.booking;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BookingGuestsValidator.class)
public @interface ValidBookingGuests {
    String message() default "Number of personal details must match guests quantity";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
