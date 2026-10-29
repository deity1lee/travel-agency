package com.epam.edp.demo.service.captcha;

import com.epam.edp.demo.dto.auth.CaptchaVerifyResponse;
import com.epam.edp.demo.exception.auth.CaptchaValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestTemplate restTemplate;
    private final String secretKey;
    private final boolean enabled;
    private final double minScore;

    public CaptchaServiceImpl(RestTemplate restTemplate,
                               @Value("${app.captcha.secret-key}") String secretKey,
                               @Value("${app.captcha.enabled:true}") boolean enabled,
                               @Value("${app.captcha.min-score:0.5}") double minScore) {
        this.restTemplate = restTemplate;
        this.secretKey = secretKey;
        this.enabled = enabled;
        this.minScore = minScore;
    }

    @Override
    public void validate(String captchaToken) {
        if (!enabled) {
            log.debug("Captcha validation is disabled, skipping");
            return;
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            throw new CaptchaValidationException("Captcha token is required");
        }

        log.info("Validating captcha token");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", captchaToken);

        CaptchaVerifyResponse response;
        try {
            response = restTemplate.postForObject(VERIFY_URL, params, CaptchaVerifyResponse.class);
        } catch (Exception e) {
            log.error("Failed to reach captcha verification service", e);
            throw new CaptchaValidationException("Captcha verification service unavailable");
        }

        if (response == null || !response.isSuccess()) {
            log.warn("Captcha verification failed. error-codes: {}",
                    response != null ? response.getErrorCodes() : "null response");
            throw new CaptchaValidationException("Captcha verification failed");
        }

        if (response.getScore() < minScore) {
            log.warn("Captcha score {} is below minimum threshold {}", response.getScore(), minScore);
            throw new CaptchaValidationException("Captcha score too low, request rejected");
        }

        log.info("Captcha validated successfully with score {}", response.getScore());
    }
}
