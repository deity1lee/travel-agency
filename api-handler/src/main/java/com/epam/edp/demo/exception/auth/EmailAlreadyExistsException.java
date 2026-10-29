package com.epam.edp.demo.exception.auth;

public class EmailAlreadyExistsException extends AuthValidationException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
