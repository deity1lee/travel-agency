package com.epam.edp.demo.exception.auth;

public class AuthValidationException extends RuntimeException {
    public AuthValidationException(String registrationRequestIsRequired) {
        super(registrationRequestIsRequired);
    }
}
