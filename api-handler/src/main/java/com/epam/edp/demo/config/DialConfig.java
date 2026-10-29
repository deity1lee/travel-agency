package com.epam.edp.demo.config;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.OpenAIServiceVersion;
import com.azure.core.credential.AzureKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Builds the Azure OpenAI client used to reach the moderation model on EPAM DIAL.
 *
 * <p>The API key is injected from the {@code DIAL_API_KEY} environment variable and is
 * never committed to source. The endpoint, model and api-version are non-secret,
 * environment-configurable properties.
 */
@Configuration
public class DialConfig {

    private final String apiKey;
    private final String endpoint;
    private final String apiVersion;

    public DialConfig(@Value("${dial.api-key}") String apiKey,
                      @Value("${dial.endpoint}") String endpoint,
                      @Value("${dial.api-version}") String apiVersion) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.apiVersion = apiVersion;
    }

    @Bean
    public OpenAIClient openAIClient() {
        return new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .serviceVersion(resolveServiceVersion(apiVersion))
                .buildClient();
    }

    private static OpenAIServiceVersion resolveServiceVersion(String configured) {
        if (!StringUtils.hasText(configured)) {
            return OpenAIServiceVersion.getLatest();
        }
        return Arrays.stream(OpenAIServiceVersion.values())
                .filter(v -> v.getVersion().equals(configured))
                .findFirst()
                .orElse(OpenAIServiceVersion.getLatest());
    }
}
