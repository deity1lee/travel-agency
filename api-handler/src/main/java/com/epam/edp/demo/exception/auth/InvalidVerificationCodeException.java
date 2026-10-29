package com.epam.edp.demo.exception.auth;

import lombok.Getter;

/**
 * Thrown when a submitted verification code does not match any pending token for the email
 * (wrong code, or no signup/email-change in progress). Maps to 400 with deliberately generic
 * copy so the verify endpoint cannot be used to enumerate which emails have a pending token.
 */
@Getter
public class InvalidVerificationCodeException extends RuntimeException {

    private final int remainingAttempts;

    public InvalidVerificationCodeException(String message, int remainingAttempts) {
        super(message);
        this.remainingAttempts = remainingAttempts;
    }
}
