package com.epam.edp.demo.service.moderation;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.epam.edp.demo.dto.moderation.ModerationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ModerationServiceImpl implements ModerationService {

    static final int DEFAULT_MAX_ATTEMPTS = 2;
    static final long DEFAULT_TIMEOUT_MS = 5_000L;

    private static final String SYSTEM_PROMPT = """
            You are a content-moderation classifier for travel-booking reviews.

            Your task is to decide whether a user-submitted review can be published.

            MOST IMPORTANT RULE: do not over-block. Critical, negative, angry, low-rated, and complaint-style reviews are legitimate feedback and must be APPROVED unless they violate one of the specific rules below. A review may be harsh, blunt, or unflattering about a hotel, tour, guide, guest experience, booking process, or service and still be acceptable.
            
            Example: "The hotel was filthy and the staff were rude" is APPROVED.

            Return ONLY a valid JSON object with this exact shape:
            {
              "status": "APPROVED" | "NEEDS_EDIT" | "REJECTED",
              "reason": "short explanation",
              "categories": ["allowed category names only"],
              "userMessage": "polite message shown to the user"
            }

            Allowed categories:
            - NONE
            - PROFANITY
            - INSULT
            - HARASSMENT
            - HATE_SPEECH
            - THREAT
            - SEXUAL_CONTENT
            - SELF_HARM
            - ILLEGAL_ACTIVITY
            - PRIVATE_INFORMATION
            - SPAM
            - OFF_TOPIC

            Decision rules:
            
            APPROVED:
            Use APPROVED for normal travel reviews, including harsh-but-civil criticism, negative opinions, complaints, bug reports, poor ratings, accusations about service quality, or descriptions of bad experiences.
            
            For APPROVED reviews, use:
            "categories": ["NONE"]
            
            NEEDS_EDIT:
            Use NEEDS_EDIT when the review is related to the travel experience but needs rewriting before publication and does not fall into a REJECTED harm category.
            
            Use NEEDS_EDIT for:

            * Mild profanity
            * Rude wording
            * Personal insults that are not targeted harassment
            * Empty, meaningless, or incoherent text
            * Spam-like content
            * Mostly off-topic content
            
            REJECTED:
            Use REJECTED only for these defined harm categories:
            
            * HATE_SPEECH: attacks, slurs, or demeaning statements targeting protected characteristics
            * HARASSMENT: targeted abuse, bullying, or repeated hostile personal attacks toward an identifiable person
            * THREAT: threats of violence, intimidation, or encouragement of physical harm
            * SEXUAL_CONTENT: explicit sexual content or sexually exploitative content
            * SELF_HARM: encouragement, instructions, or intent involving self-harm or suicide
            * ILLEGAL_ACTIVITY: promotion, solicitation, or instructions for committing illegal activity
            * PRIVATE_INFORMATION: personal data, doxxing, private contact details, addresses, payment information, identification numbers, or other sensitive private information
            
            Important:
            
            * Never reject or downgrade a review merely because it is negative, critical, angry, harsh, or low-rated.
            * Reject only for the REJECTED harm categories listed above.
            * If unsure whether content falls into a REJECTED harm category, use NEEDS_EDIT instead of REJECTED.
            * If the review is a coherent travel review and does not violate a rule, use APPROVED.
            * Do not classify allegations or complaints about a business as harassment merely because they are negative.
            
            The reason must be short, specific, and human-readable.
            
            The userMessage must be polite and actionable so the user can edit and resubmit when applicable.
            
            Do not include markdown.
            Do not include any text outside the JSON object.
            """;

    private final OpenAIClient client;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxAttempts;
    private final long timeoutMs;
    private final ExecutorService executor;

    public ModerationServiceImpl(OpenAIClient client,
                                 ObjectMapper objectMapper,
                                 @Value("${dial.model}") String model,
                                 @Value("${dial.moderation.max-attempts:" + DEFAULT_MAX_ATTEMPTS + "}") int maxAttempts,
                                 @Value("${dial.moderation.timeout-ms:" + DEFAULT_TIMEOUT_MS + "}") long timeoutMs) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.model = model;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.timeoutMs = timeoutMs;
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "moderation-call");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public ModerationResult moderate(String content) {
        if (content == null || content.isBlank()) {
            return ModerationResult.approved("No review text to moderate");
        }
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return moderateOnce(content);
            } catch (Exception e) {
                log.warn("Moderation attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
            }
        }
        log.error("Moderation unavailable after {} attempt(s), failing open (review approved)", maxAttempts);
        return ModerationResult.failOpen();
    }

    private ModerationResult moderateOnce(String content) throws Exception {
        ChatCompletionsOptions options = new ChatCompletionsOptions(buildMessages(content))
                .setTemperature(0.0);
        CompletableFuture<ChatCompletions> call =
                CompletableFuture.supplyAsync(() -> client.getChatCompletions(model, options), executor);
        try {
            ChatCompletions completions = call.get(timeoutMs, TimeUnit.MILLISECONDS);
            return parse(extractReply(completions));
        } catch (TimeoutException e) {
            call.cancel(true);
            throw new IllegalStateException("Moderation call timed out after " + timeoutMs + "ms", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : new IllegalStateException(cause);
        }
    }

    private List<ChatRequestMessage> buildMessages(String content) {
        return List.of(
                new ChatRequestSystemMessage(SYSTEM_PROMPT),
                new ChatRequestUserMessage(content)
        );
    }

    private String extractReply(ChatCompletions completions) {
        List<ChatChoice> choices = completions == null ? null : completions.getChoices();
        if (choices == null || choices.isEmpty() || choices.get(0).getMessage() == null) {
            throw new IllegalStateException("Moderation model returned no choices");
        }
        return choices.get(0).getMessage().getContent();
    }

    private ModerationResult parse(String reply) {
        ModerationResult result;
        try {
            String json = extractJsonObject(reply);
            result = objectMapper.readValue(json, ModerationResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse moderation response", e);
        }
        log.info("Moderation verdict: status={} categories={}", result.status(), result.categories());
        return result;
    }

    private String extractJsonObject(String reply) {
        if (reply == null) {
            throw new IllegalStateException("Moderation model returned empty content");
        }
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalStateException("Moderation response contained no JSON object");
        }
        return reply.substring(start, end + 1);
    }
}
