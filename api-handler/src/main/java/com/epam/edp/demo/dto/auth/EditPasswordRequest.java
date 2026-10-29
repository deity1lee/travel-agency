package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.validation.auth.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record EditPasswordRequest(
        @Schema(description = "Password", example = "PstroNg.pa4")
        @NotNull(message = "New password is required")
        @ValidPassword
        String newPassword,

        @Schema(description = "Password", example = "JstroNg.pa3")
        @NotNull(message = "Old password is required")
        String oldPassword
) {
}
