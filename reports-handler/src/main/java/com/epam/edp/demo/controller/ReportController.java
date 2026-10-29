package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.ExportFormat;
import com.epam.edp.demo.dto.ReportFile;
import com.epam.edp.demo.dto.ReportRequest;
import com.epam.edp.demo.model.AggregateReport;
import com.epam.edp.demo.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/report")
@Slf4j
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService){
        this.reportService = reportService;
    }

    //endpoint to get available locations for filter?

    @GetMapping
    public List<AggregateReport> getReports(@ModelAttribute ReportRequest reportRequest) {
        log.info("Incoming request: GET /report, {}", reportRequest);
        List<AggregateReport> aggregateReportList = reportService.getAggregatePerformanceReport(reportRequest);
        log.info("Response: 200 OK – returning report with {} items", aggregateReportList.size());
        return aggregateReportList;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> getReportExport(@ModelAttribute ReportRequest reportRequest,
                                                  @RequestParam ExportFormat format) {
        log.info("Incoming request: GET /report/export?format={}", format);

        ReportFile reportFile = reportService.exportAggregatePerformanceReport(reportRequest, format);

        log.info("Response: 200 OK – returning report bytes");
        return ResponseEntity.ok()
                .contentType(reportFile.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportFile.filename() + "\"")
                .body(reportFile.bytes());
    }
}
