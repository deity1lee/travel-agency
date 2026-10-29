package com.epam.edp.demo.dto;

import org.springframework.http.MediaType;

public record ReportFile(
        byte[] bytes,
        MediaType mediaType,
        String filename
) {
}
