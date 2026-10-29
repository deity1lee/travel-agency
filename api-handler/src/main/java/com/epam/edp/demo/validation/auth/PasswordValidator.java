package com.epam.edp.demo.validation.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern SPECIAL_CHARACTER_PATTERN =
            Pattern.compile(".*[!@#$%^&*()_+\\-\\[\\]{};':\"\\\\|,.<>/?].*");


    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {

        if (password == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (password.length() < 8 || password.length() > 16) {
            addViolation(context, "Password must be 8-16 characters long");
            valid = false;
        }

        if (!password.matches(".*[A-Z].*")) {
            addViolation(context, "At least one uppercase letter required");
            valid = false;
        }

        if (!password.matches(".*[a-z].*")) {
            addViolation(context, "At least one lowercase letter required");
            valid = false;
        }

        if (!password.matches(".*\\d.*")) {
            addViolation(context, "At least one number required");
            valid = false;
        }

        if (!SPECIAL_CHARACTER_PATTERN.matcher(password).matches()) {
            addViolation(context, "At least one special character required");
            valid = false;
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}
