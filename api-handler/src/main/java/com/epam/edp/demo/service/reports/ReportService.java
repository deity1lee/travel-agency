package com.epam.edp.demo.service.reports;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.reports.AggregateReport;
import com.epam.edp.demo.dto.reports.ExportFormat;
import com.epam.edp.demo.dto.reports.ReportRequest;
import com.epam.edp.demo.service.rest.Service2Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ReportService {

    private final Service2Client service2Client;

    public ReportService(Service2Client service2Client){
        this.service2Client = service2Client;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<AggregateReport> getAggregateReport(UserContext userContext, ReportRequest reportRequest) {
        log.info("Admin {} has requested report {}", userContext.userId(), reportRequest);
        List<AggregateReport> report = service2Client.getReport(reportRequest);
        log.info("Fetched report with {} entries for admin {}", report.size(), userContext.userId());
        return report;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]>  getAggregateReportExport(ReportRequest reportRequest, ExportFormat format) {
        log.info("Admin has requested report {} in {} format", reportRequest, format);
        ResponseEntity<byte[]> reportResponse = service2Client.getReportExport(reportRequest, format);
        log.info("Fetched report");
        return reportResponse;
    }
}
