package com.epam.edp.demo.exception.seed;

import com.epam.edp.demo.controller.DevController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@RestControllerAdvice(assignableTypes = DevController.class)
public class DevControllerAdvice {

    @ExceptionHandler(SeedingException.class)
    public ResponseEntity<String> handleSeedingException(SeedingException ex) {
        log.warn("Returning 404 NOT FOUND: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }
}
