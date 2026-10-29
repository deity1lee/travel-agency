package com.epam.edp.demo.dto.moderation;

import java.util.List;

public record ModerationRejectionResponse(
        String status,
        String action,
        String reason,
        List<String> categories,
        String userMessage
) {
}
