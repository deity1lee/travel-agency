package com.epam.edp.demo.validation.documents;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PassportNameValidator.class)
public @interface ValidDocumentHolder {

    String message() default "First name and last name are required for PASSPORT documents";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
