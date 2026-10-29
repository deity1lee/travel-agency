package com.epam.edp.demo.validation.auth;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailTldValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmailTLD {
    String message() default "Invalid email format or TLD";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
