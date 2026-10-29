package com.epam.edp.demo.validation.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;

@Slf4j
public class EmailTldValidator implements ConstraintValidator<ValidEmailTLD, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        log.debug("Validating email TLD for email: {}", value);
        if (value == null || value.isBlank()) {
            return true;
        }

        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(value);
    }
}
