package com.epam.edp.demo.service.rest;

import com.epam.edp.demo.dto.reports.AggregateReport;
import com.epam.edp.demo.dto.reports.ExportFormat;
import com.epam.edp.demo.dto.reports.ReportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.util.List;

@Service
@Slf4j
public class Service2Client {

    private final WebClient webClient;

    public Service2Client(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<AggregateReport> getReport(ReportRequest request) {
        log.info("Requesting report");
        List<AggregateReport> response = webClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/report");
                    builder.queryParam("reportType", request.reportType())
                            .queryParam("startDate", request.startDate())
                            .queryParam("endDate", request.endDate());

                    if (request.location() != null) {
                        builder.queryParam("location", request.location());
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AggregateReport>>() {})
                .block();

        log.info("Received report {}", response);
        return response;
    }

    public ResponseEntity<byte[]> getReportExport(ReportRequest request, ExportFormat format) {
        log.info("Requesting report in format {}", format);
        ResponseEntity<byte[]> response = webClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/report/export");
                    builder.queryParam("reportType", request.reportType())
                            .queryParam("startDate", request.startDate())
                            .queryParam("endDate", request.endDate())
                            .queryParam("format", format);

                    if (request.location() != null) {
                        builder.queryParam("location", request.location());
                    }
                    return builder.build();
                })
                .retrieve()
                .toEntity(byte[].class)
                .block();

        log.info("Received exported report {}", response);
        return ResponseEntity
                .status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
    }

}
