package com.epam.edp.demo.scheduling;

import com.epam.edp.demo.service.ReportServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Component
public class ReportsSender {

    private final ReportServiceImpl reportServiceImpl;
    private final Clock clock;

    public ReportsSender(
            ReportServiceImpl reportServiceImpl,
            Clock clock) {

        this.reportServiceImpl = reportServiceImpl;
        this.clock = clock;
    }

    @Scheduled(cron = "${reports.weekly.cron}", zone = "UTC")
    public void generateWeeklyReports() {
        LocalDate now = LocalDate.now(clock);
        LocalDate startDate = now.minusDays(7);
        log.info("Starting weekly reports generation for date {}", now);

        reportServiceImpl.sendWeeklyTourPerformanceReport(startDate, now);
        reportServiceImpl.sendWeeklyTravelAgentPerformanceReport(startDate, now);

        log.info("Weekly reports generation completed");
    }
}
