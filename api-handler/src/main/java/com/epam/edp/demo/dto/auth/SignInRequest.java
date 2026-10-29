package com.epam.edp.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for sign-in")
public record SignInRequest(
        @Schema(
                description = "Email",
                example = "john.doe@gmail.com"
        )
        @Email(message = "Invalid email format")
        @NotNull(message = "Email is required")
        String email,

        @Schema(
                description = "Password",
                example = "JstroNg.pa3"
        )
        @NotNull(message = "Password is required")
        String password
) {
}
