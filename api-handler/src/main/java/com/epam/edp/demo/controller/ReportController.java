package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.reports.AggregateReport;
import com.epam.edp.demo.dto.reports.ExportFormat;
import com.epam.edp.demo.dto.reports.ReportRequest;
import com.epam.edp.demo.dto.reports.ReportResponse;
import com.epam.edp.demo.service.reports.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/report")
@Slf4j
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService){
        this.reportService = reportService;
    }

    @GetMapping
    @Operation(
            summary = "Check if documents were uploaded successfully",
            description = "Checks if documents were uploaded to S3 successfully"
    )
    @ApiResponse(responseCode = "200", description = "Returned reports json")
    @ApiResponse(responseCode = "403", description = "Not enough permissions to access endpoint")
    public ReportResponse getReports(@CurrentUser UserContext userContext,
                                     @ModelAttribute ReportRequest reportRequest) {
        log.info("Incoming request: GET /admin/report, {}", reportRequest);
        List<AggregateReport> report = reportService.getAggregateReport(userContext, reportRequest);
        log.info("Response: 200 OK – returning report with {} items", report.size());
        return new ReportResponse(report);
    }

    @GetMapping("/export")
    @Operation(
            summary = "Check if documents were uploaded successfully",
            description = "Checks if documents were uploaded to S3 successfully"
    )
    @ApiResponse(responseCode = "200", description = "Returned reports in requested format")
    @ApiResponse(responseCode = "403", description = "Not enough permissions to access endpoint")
    public ResponseEntity<byte[]> getReportExport(@ModelAttribute ReportRequest reportRequest,
                                                  @RequestParam ExportFormat format) {
        log.info("Incoming request: GET /admin/report/export?format={}", format);
        ResponseEntity<byte[]> response = reportService.getAggregateReportExport(reportRequest, format);
        log.info("Response: 200 OK – returning report bytes");
        return response;
    }
}
