package com.epam.edp.demo.exception.auth;

public class CaptchaValidationException extends RuntimeException {
    public CaptchaValidationException(String message) {
        super(message);
    }
}
