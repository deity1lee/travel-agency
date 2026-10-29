package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.dto.document.PhotoUpload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

public record EditUserNamesRequest(
        @Schema(description = "First name", example = "John")
        @Pattern(
                regexp = "^[A-Za-z'-]{1,50}$",
                message = "First name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed"
        )
        String firstName,

        @Schema(description = "Last name", example = "Doe")
        @Pattern(
                regexp = "^[A-Za-z'-]{1,50}$",
                message = "Last name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed"
        )
        String lastName,

        @Schema(description = "Photo Upload")
        @Valid
        PhotoUpload photo
) {
}
