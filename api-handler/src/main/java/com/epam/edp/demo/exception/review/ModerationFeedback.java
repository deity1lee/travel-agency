package com.epam.edp.demo.exception.review;

import com.epam.edp.demo.dto.moderation.ModerationStatus;

/**
 * Maps a moderation {@link ModerationStatus} to the user-facing instruction the
 * frontend branches on. {@code NEEDS_EDIT} is a fixable outcome (revise and
 * resubmit); {@code REJECTED} is terminal (cannot be published). Keeping this
 * presentation concern in the web layer leaves {@link ModerationStatus} a pure
 * domain enum. Each status also carries a deterministic default message so the
 * "clear feedback" guarantee holds even when the model returns a blank one.
 */
final class ModerationFeedback {

    static final String ACTION_EDIT_AND_RESUBMIT = "EDIT_AND_RESUBMIT";
    static final String ACTION_BLOCKED = "BLOCKED";

    private static final String DEFAULT_EDIT_MESSAGE = "Please revise your review and resubmit.";
    private static final String DEFAULT_BLOCKED_MESSAGE = "This review can't be published.";

    private ModerationFeedback() {
    }

    static String action(ModerationStatus status) {
        return switch (status) {
            case NEEDS_EDIT -> ACTION_EDIT_AND_RESUBMIT;
            case REJECTED -> ACTION_BLOCKED;
            case APPROVED -> throw new IllegalArgumentException("APPROVED is not a rejection outcome");
        };
    }

    static String defaultMessage(ModerationStatus status) {
        return switch (status) {
            case NEEDS_EDIT -> DEFAULT_EDIT_MESSAGE;
            case REJECTED -> DEFAULT_BLOCKED_MESSAGE;
            case APPROVED -> throw new IllegalArgumentException("APPROVED is not a rejection outcome");
        };
    }
}
