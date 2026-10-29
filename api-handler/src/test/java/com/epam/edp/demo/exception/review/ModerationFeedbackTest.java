package com.epam.edp.demo.exception.review;

import com.epam.edp.demo.dto.moderation.ModerationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationFeedbackTest {

    @Test
    void action_mapsNeedsEditToEditAndResubmit() {
        assertThat(ModerationFeedback.action(ModerationStatus.NEEDS_EDIT))
                .isEqualTo("EDIT_AND_RESUBMIT");
    }

    @Test
    void action_mapsRejectedToBlocked() {
        assertThat(ModerationFeedback.action(ModerationStatus.REJECTED))
                .isEqualTo("BLOCKED");
    }

    @Test
    void action_rejectsApproved() {
        assertThatThrownBy(() -> ModerationFeedback.action(ModerationStatus.APPROVED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultMessage_isActionableForNeedsEdit() {
        assertThat(ModerationFeedback.defaultMessage(ModerationStatus.NEEDS_EDIT))
                .isEqualTo("Please revise your review and resubmit.");
    }

    @Test
    void defaultMessage_isTerminalForRejected() {
        assertThat(ModerationFeedback.defaultMessage(ModerationStatus.REJECTED))
                .isEqualTo("This review can't be published.");
    }

    @Test
    void defaultMessage_rejectsApproved() {
        assertThatThrownBy(() -> ModerationFeedback.defaultMessage(ModerationStatus.APPROVED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
