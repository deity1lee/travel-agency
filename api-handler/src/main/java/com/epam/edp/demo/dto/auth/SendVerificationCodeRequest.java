package com.epam.edp.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for verification code sending: enter email to recover password")
public record SendVerificationCodeRequest (

        @Schema(description = "Email the verification code must be sent to", example = "john.doe@gmail.com")
        @Email(message = "Invalid email format")
        @NotNull(message = "Email is required")
        String email
) {
}
