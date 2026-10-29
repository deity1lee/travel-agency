package com.epam.edp.demo.exception.tour;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class TourValidationException extends RuntimeException {
    private final Map<String, String> errors;
}