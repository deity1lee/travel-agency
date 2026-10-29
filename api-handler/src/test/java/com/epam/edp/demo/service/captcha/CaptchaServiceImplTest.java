package com.epam.edp.demo.service.captcha;

import com.epam.edp.demo.dto.auth.CaptchaVerifyResponse;
import com.epam.edp.demo.exception.auth.CaptchaValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final String SECRET_KEY = "test-secret-key";
    private static final double MIN_SCORE = 0.5;

    @Mock
    private RestTemplate restTemplate;

    private CaptchaServiceImpl service(boolean enabled) {
        return new CaptchaServiceImpl(restTemplate, SECRET_KEY, enabled, MIN_SCORE);
    }

    private CaptchaServiceImpl service() {
        return service(true);
    }

    private CaptchaVerifyResponse successResponse(double score) {
        CaptchaVerifyResponse r = new CaptchaVerifyResponse();
        r.setSuccess(true);
        r.setScore(score);
        return r;
    }

    private CaptchaVerifyResponse failedResponse() {
        CaptchaVerifyResponse r = new CaptchaVerifyResponse();
        r.setSuccess(false);
        r.setScore(0.9);
        return r;
    }

    @Test
    void validate_whenDisabled_skipsValidationWithoutCallingRestTemplate() {
        service(false).validate("any-token");

        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void validate_whenDisabled_doesNotThrowEvenForNullToken() {
        service(false).validate(null);

        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void validate_nullToken_throwsCaptchaValidationException() {
        assertThatThrownBy(() -> service().validate(null))
                .isInstanceOf(CaptchaValidationException.class)
                .hasMessage("Captcha token is required");

        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void validate_emptyToken_throwsCaptchaValidationException() {
        assertThatThrownBy(() -> service().validate(""))
                .isInstanceOf(CaptchaValidationException.class)
                .hasMessage("Captcha token is required");

        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void validate_blankToken_throwsCaptchaValidationException() {
        assertThatThrownBy(() -> service().validate("   "))
                .isInstanceOf(CaptchaValidationException.class)
                .hasMessage("Captcha token is required");

        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void validate_restTemplateThrows_throwsCaptchaValidationExceptionWithUnavailableMessage() {
        when(restTemplate.postForObject(anyString(), any(), any()))
                .thenThrow(new RuntimeException("network error"));

        assertThatThrownBy(() -> service().validate("valid-token"))
                .isInstanceOf(CaptchaValidationException.class)
                .hasMessage("Captcha verification service unavailable");
    }

    @Test
    void validate_nullResponseFromApi_throwsCaptchaValidationException() {
        when(restTemplate.postForObject(anyString(), any(), eq(CaptchaVerifyResponse.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> service().validate("valid-token"))
                .isInstanceOf(CaptchaValidationException.class)
                .hasMessage("Captcha verification failed");
    }

    @Test
    void validate_responseSuccessFalse_throwsCaptchaValidationException() {
        when(restTemplate.postForObject(anyString(), any(), eq(CaptchaVerifyResponse.class)))
                .thenReturn(failedResponse());

        assertThatThrownBy(() -> service().validate("valid-token"))
                .isInstanceOf(CaptchaValidationException.class)
                .hasMessage("Captcha verification failed");
    }

    @Test
    void validate_scoreBelowThreshold_throwsCaptchaValidationException() {
        when(restTemplate.postForObject(anyString(), any(), eq(CaptchaVerifyResponse.class)))
                .thenReturn(successResponse(0.49));

        assertThatThrownBy(() -> service().validate("valid-token"))
                .isInstanceOf(CaptchaValidationException.class)
                .hasMessage("Captcha score too low, request rejected");
    }

    @Test
    void validate_scoreExactlyAtThreshold_succeeds() {
        when(restTemplate.postForObject(anyString(), any(), eq(CaptchaVerifyResponse.class)))
                .thenReturn(successResponse(0.5));

        service().validate("valid-token"); // must not throw
    }

    @Test
    void validate_scoreAboveThreshold_succeeds() {
        when(restTemplate.postForObject(anyString(), any(), eq(CaptchaVerifyResponse.class)))
                .thenReturn(successResponse(0.9));

        service().validate("valid-token"); // must not throw
    }

    @Test
    @SuppressWarnings("unchecked")
    void validate_sendsCorrectSecretAndTokenToGoogleApi() {
        when(restTemplate.postForObject(anyString(), any(), eq(CaptchaVerifyResponse.class)))
                .thenReturn(successResponse(0.9));

        service().validate("my-captcha-token");

        ArgumentCaptor<MultiValueMap<String, String>> paramsCaptor =
                ArgumentCaptor.forClass(MultiValueMap.class);
        verify(restTemplate).postForObject(
                eq(VERIFY_URL),
                paramsCaptor.capture(),
                eq(CaptchaVerifyResponse.class));

        MultiValueMap<String, String> params = paramsCaptor.getValue();
        assertThat(params.getFirst("secret")).isEqualTo(SECRET_KEY);
        assertThat(params.getFirst("response")).isEqualTo("my-captcha-token");
    }
}
