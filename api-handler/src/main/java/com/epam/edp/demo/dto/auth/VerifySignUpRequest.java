package com.epam.edp.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body for step 2 of verified registration: submit the emailed code")
public record VerifySignUpRequest(

        @Schema(description = "Email the verification code was sent to", example = "john.doe@gmail.com")
        @Email(message = "Invalid email format")
        @NotNull(message = "Email is required")
        String email,

        @Schema(description = "6-digit verification code from the email", example = "483921")
        @NotNull(message = "Verification code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
        String code
) {
}
