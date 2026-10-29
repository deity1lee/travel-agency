package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.validation.auth.ValidEmailTLD;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record EditEmailRequest(
        @Schema(description = "Email", example = "john.doe@gmail.com")
        @Email(message = "Invalid email format")
        @NotNull(message = "Email is required")
        @ValidEmailTLD
        String newEmail,

        @Schema(description = "Password", example = "JstroNg.pa3")
        @NotNull(message = "Password is required")
        String password
) {
}
