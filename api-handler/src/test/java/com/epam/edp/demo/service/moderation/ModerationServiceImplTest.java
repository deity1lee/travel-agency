package com.epam.edp.demo.service.moderation;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.epam.edp.demo.dto.moderation.ModerationResult;
import com.epam.edp.demo.dto.moderation.ModerationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationServiceImplTest {

    private static final String MODEL = "gpt-4o";
    private static final int MAX_ATTEMPTS = 2;
    private static final long TIMEOUT_MS = 5_000L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAIClient client = mock(OpenAIClient.class);
    private final ModerationService service =
            new ModerationServiceImpl(client, objectMapper, MODEL, MAX_ATTEMPTS, TIMEOUT_MS);

    @Test
    void approvedJsonResponseYieldsApprovedVerdict() {
        stubReply("{\"status\":\"APPROVED\",\"reason\":\"Clean review\","
                + "\"categories\":[\"NONE\"],\"userMessage\":\"Thanks for your review!\"}");

        ModerationResult result = service.moderate("Lovely tour, great guide!");

        assertThat(result.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(result.reason()).isEqualTo("Clean review");
        assertThat(result.categories()).containsExactly("NONE");
        assertThat(result.userMessage()).isEqualTo("Thanks for your review!");
    }

    @Test
    void harshButCivilNegativeReviewIsApproved() {
        // Anti-over-blocking: legitimate negative feedback must not be rejected.
        stubReply("{\"status\":\"APPROVED\",\"reason\":\"Negative but civil criticism\","
                + "\"categories\":[\"NONE\"],\"userMessage\":\"Thanks for your feedback.\"}");

        ModerationResult result =
                service.moderate("The hotel was filthy and the staff were rude and unhelpful.");

        assertThat(result.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(result.isApproved()).isTrue();
        assertThat(result.isModerationUnavailable()).isFalse();
    }

    @Test
    void harmCategoryContentIsRejectedWithViolatedCategoryPopulated() {
        stubReply("{\"status\":\"REJECTED\",\"reason\":\"Hate speech targeting a group\","
                + "\"categories\":[\"HATE_SPEECH\"],"
                + "\"userMessage\":\"This review cannot be published. Please edit and resubmit.\"}");

        ModerationResult result = service.moderate("some hateful content");

        assertThat(result.status()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(result.categories()).containsExactly("HATE_SPEECH");
        assertThat(result.reason()).isNotBlank();
    }

    @Test
    void needsEditJsonResponseYieldsNeedsEditVerdict() {
        stubReply("{\"status\":\"NEEDS_EDIT\",\"reason\":\"Mild profanity\","
                + "\"categories\":[\"PROFANITY\"],"
                + "\"userMessage\":\"Please remove the offensive language and resubmit.\"}");

        ModerationResult result = service.moderate("the tour was damn awful");

        assertThat(result.status()).isEqualTo(ModerationStatus.NEEDS_EDIT);
        assertThat(result.reason()).isEqualTo("Mild profanity");
        assertThat(result.categories()).containsExactly("PROFANITY");
        assertThat(result.userMessage())
                .isEqualTo("Please remove the offensive language and resubmit.");
    }

    @Test
    void rejectedJsonResponseYieldsRejectedVerdictWithReasonAndCategories() {
        stubReply("{\"status\":\"REJECTED\",\"reason\":\"Contains a threat\","
                + "\"categories\":[\"THREAT\",\"HARASSMENT\"],"
                + "\"userMessage\":\"This review cannot be published.\"}");

        ModerationResult result = service.moderate("some abusive content");

        assertThat(result.status()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(result.reason()).isEqualTo("Contains a threat");
        assertThat(result.categories()).containsExactly("THREAT", "HARASSMENT");
        assertThat(result.userMessage()).isEqualTo("This review cannot be published.");
    }

    @Test
    void blankContentApprovedWithoutCallingTheModel() {
        ModerationResult result = service.moderate("   ");

        assertThat(result.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(result.isApproved()).isTrue();
        verify(client, never()).getChatCompletions(any(), any());
    }

    @Test
    void nullContentApprovedWithoutCallingTheModel() {
        ModerationResult result = service.moderate(null);

        assertThat(result.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(result.isApproved()).isTrue();
        verify(client, never()).getChatCompletions(any(), any());
    }

    @Test
    void modelFailureFailsOpenToApproved() {
        when(client.getChatCompletions(eq(MODEL), any(ChatCompletionsOptions.class)))
                .thenThrow(new RuntimeException("DIAL is down"));

        ModerationResult result = service.moderate("Lovely tour, great guide!");

        assertThat(result.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(result.isModerationUnavailable()).isTrue();
        assertThat(result.categories()).containsExactly(ModerationResult.MODERATION_UNAVAILABLE);
    }

    @Test
    void modelFailureIsRetriedUpToMaxAttemptsBeforeFailingOpen() {
        when(client.getChatCompletions(eq(MODEL), any(ChatCompletionsOptions.class)))
                .thenThrow(new RuntimeException("DIAL is down"));

        ModerationResult result = service.moderate("Lovely tour, great guide!");

        assertThat(result.isModerationUnavailable()).isTrue();
        verify(client, times(MAX_ATTEMPTS))
                .getChatCompletions(eq(MODEL), any(ChatCompletionsOptions.class));
    }

    @Test
    void transientFailureThenSuccessReturnsTheVerdictWithoutFailingOpen() {
        when(client.getChatCompletions(eq(MODEL), any(ChatCompletionsOptions.class)))
                .thenThrow(new RuntimeException("transient DIAL blip"))
                .thenReturn(completionsWith("{\"status\":\"REJECTED\",\"reason\":\"Contains a threat\","
                        + "\"categories\":[\"THREAT\"],\"userMessage\":\"This review cannot be published.\"}"));

        ModerationResult result = service.moderate("some abusive content");

        assertThat(result.status()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(result.isModerationUnavailable()).isFalse();
        verify(client, times(MAX_ATTEMPTS))
                .getChatCompletions(eq(MODEL), any(ChatCompletionsOptions.class));
    }

    @Test
    void slowModelCallTimesOutAndFailsOpen() {
        ModerationService impatientService =
                new ModerationServiceImpl(client, objectMapper, MODEL, MAX_ATTEMPTS, 50L);
        when(client.getChatCompletions(eq(MODEL), any(ChatCompletionsOptions.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(500L);
                    return completionsWith("{\"status\":\"APPROVED\",\"reason\":\"ok\","
                            + "\"categories\":[\"NONE\"],\"userMessage\":\"\"}");
                });

        ModerationResult result = impatientService.moderate("Lovely tour, great guide!");

        assertThat(result.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(result.isModerationUnavailable()).isTrue();
    }

    @Test
    void responseWithoutChoicesFailsOpenToApproved() {
        when(client.getChatCompletions(eq(MODEL), any(ChatCompletionsOptions.class)))
                .thenReturn(completionsWithoutChoices());

        ModerationResult result = service.moderate("Lovely tour, great guide!");

        assertThat(result.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(result.isModerationUnavailable()).isTrue();
    }

    @Test
    void unparseableReplyFailsOpenToApproved() {
        stubReply("not json at all");

        ModerationResult result = service.moderate("Lovely tour, great guide!");

        assertThat(result.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(result.isModerationUnavailable()).isTrue();
    }

    @Test
    void missingUserMessageInResponseNormalizesToEmptyString() {
        stubReply("{\"status\":\"APPROVED\",\"reason\":\"Clean review\",\"categories\":[\"NONE\"]}");

        ModerationResult result = service.moderate("Lovely tour, great guide!");

        assertThat(result.userMessage()).isEmpty();
    }

    @Test
    void genuineApprovalIsNotMarkedModerationUnavailable() {
        stubReply("{\"status\":\"APPROVED\",\"reason\":\"Clean review\","
                + "\"categories\":[\"NONE\"],\"userMessage\":\"Thanks for your review!\"}");

        ModerationResult result = service.moderate("Lovely tour, great guide!");

        assertThat(result.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(result.isModerationUnavailable()).isFalse();
    }

    private void stubReply(String assistantContent) {
        when(client.getChatCompletions(eq(MODEL), any(ChatCompletionsOptions.class)))
                .thenReturn(completionsWith(assistantContent));
    }

    /** Builds a real {@link ChatCompletions} whose single choice carries the given assistant text. */
    private ChatCompletions completionsWith(String assistantContent) {
        try {
            String quoted = objectMapper.writeValueAsString(assistantContent);
            String json = "{\"id\":\"test\",\"created\":0,\"choices\":[{\"index\":0,"
                    + "\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":"
                    + quoted + "}}]}";
            try (JsonReader reader = JsonProviders.createReader(json)) {
                return ChatCompletions.fromJson(reader);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Builds a real {@link ChatCompletions} with an empty choices array. */
    private ChatCompletions completionsWithoutChoices() {
        try {
            String json = "{\"id\":\"test\",\"created\":0,\"choices\":[]}";
            try (JsonReader reader = JsonProviders.createReader(json)) {
                return ChatCompletions.fromJson(reader);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
