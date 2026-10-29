package com.epam.edp.demo.dto.bookings;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CancelBookingRequest(
        @Schema(description = "Reason of cancellation by travel agent", example = "Some valid reason")
        @NotBlank(message = "Reason of cancellation mus tbe specified")
        String reason
){}
