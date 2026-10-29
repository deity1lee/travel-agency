package com.epam.edp.demo.exception;

public class NoAvailableSpotsException extends RuntimeException {
    public NoAvailableSpotsException(String message) {
        super(message);
    }
}
