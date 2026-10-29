package com.epam.edp.demo.service.email;

import com.epam.edp.demo.exception.EmailSendingDisabledException;
import com.epam.edp.demo.exception.auth.EmailSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";

    private final RestTemplate restTemplate;
    private final String from;
    private final String apiKey;
    private final boolean emailSendEnabled;

    public EmailService(RestTemplate restTemplate,
                        @Value("${app.mail.from}") String from,
                        @Value("${app.sendgrid.api-key}") String apiKey,
                        @Value("${email.verification.enabled}") boolean emailSendEnabled) {
        this.restTemplate = restTemplate;
        this.from = from;
        this.apiKey = apiKey;
        this.emailSendEnabled = emailSendEnabled;
    }

    public void sendSignupVerificationEmail(String emailAddress, String code) {
        String subject = "Travel Agency: Verify your email";
        String message = "Your verification code is: " + code
                + ". Enter it in the Travel Agency app to verify your email. This code expires in 15 minutes.";
        sendEmail(emailAddress, subject, message);
    }

    public void sendChangeEmailEmail(String emailAddress, String code) {
        String subject = "Travel Agency: Verify new email";
        String message = "Input the code in the travel agency app to verify your new email account (this is definitely not a scam): " + code;
        sendEmail(emailAddress, subject, message);
    }

    public void sendPasswordResetVerificationEmail(String emailAddress, String code) {
        String subject = "Travel Agency: Verification code for password reset";
        String message = "Input the code in the travel agency app to be able to change your password (this is definitely not a scam): " + code;
        sendEmail(emailAddress, subject, message);
    }

    private void sendEmail(String emailAddress, String subject, String message) {
        checkEmailSendingEnabled();

        Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of(
                        "to", List.of(Map.of("email", emailAddress)))),
                "from", Map.of("email", from),
                "subject", subject,
                "content", List.of(Map.of(
                        "type", "text/plain",
                        "value", message)));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(SENDGRID_URL, new HttpEntity<>(body, headers), String.class);
        } catch (HttpStatusCodeException ex) {
            throw new EmailSendException("SendGrid send failed with status "
                    + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (ResourceAccessException ex) {
            throw new EmailSendException("SendGrid send failed (network/timeout): "
                    + ex.getMessage(), ex);
        }
    }

    private void checkEmailSendingEnabled() {
        if (!emailSendEnabled) {
            throw new EmailSendingDisabledException("Email sending is currently disabled for testing purposes, please use /dev/code to get verification token");
        }
    }
}
