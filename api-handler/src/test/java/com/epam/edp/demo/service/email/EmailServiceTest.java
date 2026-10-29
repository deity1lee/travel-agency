package com.epam.edp.demo.service.email;

import com.epam.edp.demo.exception.auth.EmailSendException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private static final String FROM = "sender@example.com";
    private static final String API_KEY = "SG.test-key";
    private static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";
    private static final boolean ENABLED = true;

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final EmailService service = new EmailService(restTemplate, FROM, API_KEY, ENABLED);

    @Test
    @SuppressWarnings("unchecked")
    void sendSignupVerificationEmail_postsToSendGridWithCodeAndExpiryNotice() {
        service.sendSignupVerificationEmail("foo@bar.com", "483921");

        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(SENDGRID_URL), captor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> request = captor.getValue();
        HttpHeaders headers = request.getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + API_KEY);
        assertThat(headers.getContentType()).isNotNull();
        assertThat(headers.getContentType().toString()).startsWith("application/json");

        Map<String, Object> body = request.getBody();
        assertThat(((Map<String, Object>) body.get("from")).get("email")).isEqualTo(FROM);
        assertThat(body.get("subject")).isEqualTo("Travel Agency: Verify your email");

        List<Map<String, Object>> personalizations = (List<Map<String, Object>>) body.get("personalizations");
        List<Map<String, Object>> to = (List<Map<String, Object>>) personalizations.get(0).get("to");
        assertThat(to.get(0).get("email")).isEqualTo("foo@bar.com");

        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        assertThat(content.get(0).get("type")).isEqualTo("text/plain");
        String value = (String) content.get(0).get("value");
        assertThat(value).contains("483921");
        assertThat(value).contains("15 minutes");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendChangeEmailEmail_postsToSendGridWithCode() {
        service.sendChangeEmailEmail("new@bar.com", "112233");

        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(SENDGRID_URL), captor.capture(), eq(String.class));

        Map<String, Object> body = captor.getValue().getBody();
        assertThat(body.get("subject")).isEqualTo("Travel Agency: Verify new email");

        List<Map<String, Object>> personalizations = (List<Map<String, Object>>) body.get("personalizations");
        List<Map<String, Object>> to = (List<Map<String, Object>>) personalizations.get(0).get("to");
        assertThat(to.get(0).get("email")).isEqualTo("new@bar.com");

        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        assertThat((String) content.get(0).get("value")).contains("112233");
    }

    @Test
    void send_whenSendGridReturnsNon2xx_throwsEmailSendException() {
        when(restTemplate.postForEntity(eq(SENDGRID_URL), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.FORBIDDEN, "Forbidden", new HttpHeaders(),
                        "{\"errors\":[{\"message\":\"The from address does not match a verified Sender Identity.\"}]}".getBytes(),
                        null));

        assertThatThrownBy(() -> service.sendSignupVerificationEmail("foo@bar.com", "483921"))
                .isInstanceOf(EmailSendException.class);
    }

    @Test
    void send_whenNetworkError_throwsEmailSendException() {
        when(restTemplate.postForEntity(eq(SENDGRID_URL), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        assertThatThrownBy(() -> service.sendSignupVerificationEmail("foo@bar.com", "483921"))
                .isInstanceOf(EmailSendException.class);
    }
}
