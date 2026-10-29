package com.epam.edp.demo.dto;


import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ReportRequest(
        @NotNull(message = "Report type should be specified")
        ReportType reportType,

        @NotNull(message = "Report start date must be specified")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @NotNull(message = "Report end date must be specified")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,
        
        String location
) {
}
