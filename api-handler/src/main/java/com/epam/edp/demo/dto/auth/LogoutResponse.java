package com.epam.edp.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Logout response")
public record LogoutResponse(
        @Schema(
                description = "Logout message",
                example = "Successfully logged out"
        )
        String message
) {
}
