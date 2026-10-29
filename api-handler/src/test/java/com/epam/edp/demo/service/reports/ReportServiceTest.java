package com.epam.edp.demo.service.reports;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.reports.AggregateReport;
import com.epam.edp.demo.dto.reports.ExportFormat;
import com.epam.edp.demo.dto.reports.ReportRequest;
import com.epam.edp.demo.dto.reports.ReportType;
import com.epam.edp.demo.dto.reports.SalesAggregateReport;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.service.rest.Service2Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private Service2Client service2Client;

    @InjectMocks
    private ReportService service;

    @Test
    void getAggregateReport_delegatesToClientAndReturnsReport() {
        UserContext ctx = new UserContext("admin-123", "admin@bar.com", Role.ADMIN);
        ReportRequest request = new ReportRequest(
                ReportType.SALES,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                "Paris"
        );
        SalesAggregateReport entry = new SalesAggregateReport();
        entry.setTourName("City Tour");
        List<AggregateReport> expected = List.of(entry);
        when(service2Client.getReport(request)).thenReturn(expected);

        List<AggregateReport> result = service.getAggregateReport(ctx, request);

        verify(service2Client).getReport(request);
        assertThat(result).isSameAs(expected);
        assertThat(result).hasSize(1);
    }

    @Test
    void getAggregateReportExport_delegatesToClientAndReturnsResponseEntity() {
        ReportRequest request = new ReportRequest(
                ReportType.SALES,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                "Paris"
        );
        ExportFormat format = ExportFormat.CSV;
        ResponseEntity<byte[]> expected = ResponseEntity.status(HttpStatus.OK).body("report".getBytes());
        when(service2Client.getReportExport(request, format)).thenReturn(expected);

        ResponseEntity<byte[]> result = service.getAggregateReportExport(request, format);

        verify(service2Client).getReportExport(request, format);
        assertThat(result).isSameAs(expected);
    }
}
