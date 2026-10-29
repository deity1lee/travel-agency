package com.epam.edp.demo.dto.document;

import com.epam.edp.demo.entity.document.FileFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PhotoUpload(
        @Schema(description = "File format", example = "png")
        @NotNull(message = "File format is required")
        FileFormat fileFormat,

        @Schema(description = "File size", example = "100")
        @Min(value = 10, message = "File can be minimum 10KB")
        @Max(value = 10000, message = "File can be maximum 10.0000KB")
        @NotNull(message = "File size is required")
        Integer fileSize
) {
}
