package com.epam.edp.demo.service;

import com.epam.edp.demo.exception.EmailProcessingException;
import com.epam.edp.demo.export.CsvColumnOrder;
import com.epam.edp.demo.model.AggregateReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesEmailServiceTest {

    @Mock
    private SesClient sesClient;

    private SesEmailService sesEmailService;

    @BeforeEach
    void setUp() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(new StringTemplateResolver());
        sesEmailService = new SesEmailService(sesClient, templateEngine, "admin@example.com");
    }

    @Test
    void test_sendReportEmail_sendsRawEmail_whenInputIsValid() {
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class)))
                .thenReturn(SendRawEmailResponse.builder().messageId("m-1").build());

        sesEmailService.sendReportEmail(
                List.<AggregateReport>of(),
                "col1\nvalue1".getBytes(StandardCharsets.UTF_8),
                "weekly-report",
                "agent@example.com",
                () -> List.of("col1")
        );

        ArgumentCaptor<SendRawEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendRawEmailRequest.class);
        verify(sesClient).sendRawEmail(requestCaptor.capture());

        SendRawEmailRequest request = requestCaptor.getValue();
        assertNotNull(request);
        assertNotNull(request.rawMessage());
        assertTrue(request.rawMessage().data().asByteArray().length > 0);
    }

    @Test
    void test_sendReportEmail_wrapsException_whenSesClientThrows() {
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class)))
                .thenThrow(new RuntimeException("ses unavailable"));

        EmailProcessingException ex = assertThrows(
                EmailProcessingException.class,
                () -> sesEmailService.sendReportEmail(
                        List.<AggregateReport>of(),
                        "col1\nvalue1".getBytes(StandardCharsets.UTF_8),
                        "weekly-report",
                        "agent@example.com",
                        () -> List.of("col1")
                )
        );

        assertEquals("Email sending failed", ex.getMessage());
        assertEquals("ses unavailable", ex.getCause().getMessage());
    }

    @Test
    void test_sendReportEmail_wrapsException_whenColumnOrderIsNull() {
        EmailProcessingException ex = assertThrows(
                EmailProcessingException.class,
                () -> sesEmailService.sendReportEmail(
                        List.<AggregateReport>of(),
                        "col1\nvalue1".getBytes(StandardCharsets.UTF_8),
                        "weekly-report",
                        "agent@example.com",
                        null
                )
        );

        assertEquals("Email sending failed", ex.getMessage());
        assertNotNull(ex.getCause());
    }
}
