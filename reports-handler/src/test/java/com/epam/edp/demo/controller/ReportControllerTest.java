package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.ExportFormat;
import com.epam.edp.demo.dto.ReportFile;
import com.epam.edp.demo.dto.ReportRequest;
import com.epam.edp.demo.dto.ReportType;
import com.epam.edp.demo.model.AggregateReport;
import com.epam.edp.demo.model.SalesAggregateReport;
import com.epam.edp.demo.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    void test_getReports_returnsAggregateReportList_whenServiceReturnsData() {
        ReportRequest request = new ReportRequest(
                ReportType.SALES,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 7),
                null
        );

        AggregateReport report = new SalesAggregateReport();
        List<AggregateReport> expected = List.of(report);
        when(reportService.getAggregatePerformanceReport(request)).thenReturn(expected);

        List<AggregateReport> actual = reportController.getReports(request);

        assertEquals(expected, actual);
        verify(reportService).getAggregatePerformanceReport(request);
    }

    @Test
    void test_getReports_propagatesException_whenServiceFails() {
        ReportRequest request = new ReportRequest(
                ReportType.STAFF_PERFORMANCE,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 8),
                null
        );

        RuntimeException expected = new RuntimeException("service failure");
        when(reportService.getAggregatePerformanceReport(request)).thenThrow(expected);

        RuntimeException actual = assertThrows(RuntimeException.class, () -> reportController.getReports(request));

        assertEquals("service failure", actual.getMessage());
        verify(reportService).getAggregatePerformanceReport(request);
    }

    @Test
    void test_getReportExport_returnsResponseWithHeadersAndBody_whenExportSucceeds() {
        ReportRequest request = new ReportRequest(
                ReportType.SALES,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 8),
                "Yerevan, Armenia"
        );
        byte[] bytes = "col1,col2".getBytes();
        ReportFile reportFile = new ReportFile(bytes, MediaType.parseMediaType("text/csv"), "sales_report.csv");
        when(reportService.exportAggregatePerformanceReport(request, ExportFormat.CSV)).thenReturn(reportFile);

        ResponseEntity<byte[]> response = reportController.getReportExport(request, ExportFormat.CSV);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.parseMediaType("text/csv"), response.getHeaders().getContentType());
        assertEquals("attachment; filename=\"sales_report.csv\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals(bytes, response.getBody());
        verify(reportService).exportAggregatePerformanceReport(request, ExportFormat.CSV);
    }

    @Test
    void test_getReportExport_propagatesException_whenExportFails() {
        ReportRequest request = new ReportRequest(
                ReportType.SALES,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 8),
                null
        );

        IllegalArgumentException expected = new IllegalArgumentException("unsupported format");
        when(reportService.exportAggregatePerformanceReport(request, ExportFormat.PDF)).thenThrow(expected);

        IllegalArgumentException actual = assertThrows(
                IllegalArgumentException.class,
                () -> reportController.getReportExport(request, ExportFormat.PDF)
        );

        assertEquals("unsupported format", actual.getMessage());
        verify(reportService).exportAggregatePerformanceReport(request, ExportFormat.PDF);
    }
}

