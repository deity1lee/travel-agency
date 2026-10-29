package com.epam.edp.demo.exception.review;

import com.epam.edp.demo.dto.moderation.ModerationResult;
import com.epam.edp.demo.dto.moderation.ModerationStatus;

import java.util.List;

/**
 * Thrown when AI moderation declines to publish a review's text. Carries the
 * verdict's status, human-readable reason, violated categories, and a polite
 * user-facing message so the controller advice can surface a transparent
 * {@code 422} response while the rejected text is never persisted.
 */
public class ReviewModerationRejectedException extends RuntimeException {

    private final ModerationStatus status;
    private final List<String> categories;
    private final String userMessage;

    public ReviewModerationRejectedException(ModerationResult result) {
        super(result.reason());
        this.status = result.status();
        this.categories = result.categories();
        this.userMessage = result.userMessage();
    }

    public ModerationStatus status() {
        return status;
    }

    public String reason() {
        return getMessage();
    }

    public List<String> categories() {
        return categories;
    }

    public String userMessage() {
        return userMessage;
    }
}
