package com.epam.edp.demo.service.rest;

import com.epam.edp.demo.dto.reports.AggregateReport;
import com.epam.edp.demo.dto.reports.ExportFormat;
import com.epam.edp.demo.dto.reports.ReportRequest;
import com.epam.edp.demo.dto.reports.ReportType;
import com.epam.edp.demo.dto.reports.SalesAggregateReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Service2ClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec uriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec headersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private Service2Client service2Client;

    private static ReportRequest requestWithLocation() {
        return new ReportRequest(
                ReportType.SALES,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                "Paris"
        );
    }

    private static ReportRequest requestWithoutLocation() {
        return new ReportRequest(
                ReportType.SALES,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                null
        );
    }

    private static AggregateReport sampleReport() {
        SalesAggregateReport report = new SalesAggregateReport();
        report.setTourName("Eiffel Tour");
        report.setToursSold(42);
        return report;
    }

    // ---------------------------------------------------------------------
    // getReport
    // ---------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getReport_returnsBodyFromWebClient() {
        List<AggregateReport> stubbed = List.of(sampleReport());
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(stubbed));

        List<AggregateReport> result = service2Client.getReport(requestWithLocation());

        assertThat(result).isSameAs(stubbed);
        assertThat(result).containsExactlyElementsOf(stubbed);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReport_withNullLocation() {
        List<AggregateReport> stubbed = List.of(sampleReport());
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(stubbed));

        List<AggregateReport> result = service2Client.getReport(requestWithoutLocation());

        assertThat(result).isSameAs(stubbed);
    }

    // ---------------------------------------------------------------------
    // getReportExport
    // ---------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getReportExport_returnsEntityFromWebClient() {
        byte[] payload = "report-bytes".getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Custom", "value");
        ResponseEntity<byte[]> entity = ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .body(payload);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(byte[].class)).thenReturn(Mono.just(entity));

        ResponseEntity<byte[]> result =
                service2Client.getReportExport(requestWithLocation(), ExportFormat.PDF);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(payload);
        assertThat(result.getHeaders().getFirst("X-Custom")).isEqualTo("value");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReportExport_withNullLocation() {
        byte[] payload = "csv-bytes".getBytes();
        ResponseEntity<byte[]> entity = ResponseEntity.ok().body(payload);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(byte[].class)).thenReturn(Mono.just(entity));

        ResponseEntity<byte[]> result =
                service2Client.getReportExport(requestWithoutLocation(), ExportFormat.CSV);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(payload);
    }
}
