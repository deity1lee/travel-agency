package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.validation.auth.ValidEmailTLD;
import com.epam.edp.demo.validation.auth.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body for sign-up")
public record SignUpRequest(

        @Schema(description = "Email", example = "john.doe@gmail.com")
        @Email(message = "Invalid email format")
        @NotNull(message = "Email is required")
        @ValidEmailTLD
        String email,

        @Schema(description = "Password", example = "JstroNg.pa3")
        @NotNull(message = "Password is required")
        @ValidPassword
        String password,

        @Schema(description = "First name", example = "John")
        @Pattern(
                regexp = "^[A-Za-z'-]{1,50}$",
                message = "First name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed"
        )
        @NotNull(message = "First name is required")
        String firstName,

        @Schema(description = "Last name", example = "Doe")
        @Pattern(
                regexp = "^[A-Za-z'-]{1,50}$",
                message = "Last name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed"
        )
        @NotNull(message = "Last name is required")
        String lastName,

        @Schema(description = "reCAPTCHA v3 token from the client", example = "03AGdBq25...")
        String captchaToken
) {
}
