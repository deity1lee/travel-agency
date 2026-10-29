package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.entity.auth.VerificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record ResendCodeRequest(
        @Schema(description = "Email the verification code should be resent to", example = "john.doe@gmail.com")
        @Email(message = "Invalid email format")
        @NotNull(message = "Email is required")
        String email,
        @Schema(description = "Type of verification code to resend", example = "SIGNUP")
        @NotNull(message = "Verification type is required")
        VerificationType verificationType
) {
}
