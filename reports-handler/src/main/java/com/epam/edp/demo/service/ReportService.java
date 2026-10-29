package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.ExportFormat;
import com.epam.edp.demo.dto.ReportFile;
import com.epam.edp.demo.dto.ReportRequest;
import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.dto.FeedbackEvent;
import com.epam.edp.demo.model.AggregateReport;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    void sendWeeklyTourPerformanceReport(LocalDate startDate, LocalDate endDate);

    void sendWeeklyTravelAgentPerformanceReport(LocalDate startDate, LocalDate endDate);

    void aggregateBookingReportData(BookingStatusChangedEvent dto);

    void aggregateReviewReportData(FeedbackEvent dto);

    List<AggregateReport> getAggregatePerformanceReport(ReportRequest reportRequest);

    ReportFile exportAggregatePerformanceReport(ReportRequest reportRequest, ExportFormat format);
}

