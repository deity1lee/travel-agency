package com.epam.edp.demo.dto.reports;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ReportRequest(
        @Schema(description = "Report type that should be returned", example = "SALES")
        @NotNull(message = "Report type should be specified")
        ReportType reportType,

        @Schema(description = "Start Date of the report", example = "2026-06-01")
        @NotNull(message = "Report start date must be specified")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @Schema(description = "End Date of the report", example = "2026-07-01")
        @NotNull(message = "Report end date must be specified")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        String location
) {
}
