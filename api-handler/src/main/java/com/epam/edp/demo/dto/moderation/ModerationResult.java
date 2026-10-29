package com.epam.edp.demo.dto.moderation;

import java.util.List;

public record ModerationResult(
        ModerationStatus status,
        String reason,
        List<String> categories,
        String userMessage
) {

    public static final String MODERATION_UNAVAILABLE = "MODERATION_UNAVAILABLE";

    public ModerationResult {
        categories = categories == null ? List.of() : List.copyOf(categories);
        userMessage = userMessage == null ? "" : userMessage;
    }

    public boolean isApproved() {
        return status == ModerationStatus.APPROVED;
    }

    public boolean isModerationUnavailable() {
        return categories.contains(MODERATION_UNAVAILABLE);
    }

    public static ModerationResult approved(String reason) {
        return new ModerationResult(ModerationStatus.APPROVED, reason, List.of(), "");
    }

    public static ModerationResult failOpen() {
        return new ModerationResult(
                ModerationStatus.APPROVED,
                "",
                List.of(MODERATION_UNAVAILABLE),
                "");
    }
}
