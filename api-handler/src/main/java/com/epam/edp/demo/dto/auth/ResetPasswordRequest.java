package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.validation.auth.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for resetting password")
public record ResetPasswordRequest(

        @Schema(description = "Email the verification code was sent to", example = "john.doe@gmail.com")
        @Email(message = "Invalid email format")
        @NotNull(message = "Email is required")
        String email,

        @Schema(description = "Password", example = "PstroNg.pa4")
        @NotNull(message = "New password is required")
        @ValidPassword
        String newPassword
) {
}

