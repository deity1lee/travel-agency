package com.epam.edp.demo.exception.seed;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class DevControllerAdviceTest {

    private final DevControllerAdvice advice = new DevControllerAdvice();

    @Test
    void handleSeedingException_returns404WithMessageBody() {
        ResponseEntity<String> response = advice.handleSeedingException(
                new SeedingException("seed file not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("seed file not found");
    }

    @Test
    void handleSeedingException_propagatesNullMessage() {
        ResponseEntity<String> response = advice.handleSeedingException(new SeedingException(null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }
}
