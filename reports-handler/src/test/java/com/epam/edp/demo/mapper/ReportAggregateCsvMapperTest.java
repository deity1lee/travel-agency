package com.epam.edp.demo.mapper;

import com.epam.edp.demo.exception.ExportException;
import com.epam.edp.demo.export.PerformanceReportColumnOrder;
import com.epam.edp.demo.export.SalesReportColumnOrder;
import com.epam.edp.demo.export.SalesStatisticsReportCsv;
import com.epam.edp.demo.export.TravelAgentPerformanceReportCsv;
import com.epam.edp.demo.model.PerformanceAggregateReport;
import com.epam.edp.demo.model.SalesAggregateReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportAggregateCsvMapperTest {

    @Test
    void test_toCsv_generatesCsvWithOrderedHeadersAndFormattedValues_whenSalesDataIsValid() {
        SalesStatisticsReportCsv row = SalesStatisticsReportCsv.builder()
                .tourName("City Tour")
                .destinationCountry("France")
                .destinationCity("Paris")
                .reportStartDate(LocalDate.of(2026, 6, 27))
                .reportEndDate(LocalDate.of(2026, 7, 4))
                .toursSold(10)
                .tourSoldDelta(BigDecimal.valueOf(12.34))
                .feedbackRateAvg(BigDecimal.valueOf(4.5))
                .feedbackRateMin(3)
                .feedbackRateAvgDelta(BigDecimal.valueOf(-2.5))
                .finishedToursRevenue(BigDecimal.valueOf(1500.0))
                .finishedToursRevenueDelta(BigDecimal.valueOf(25.0))
                .build();

        byte[] csv = ReportAggregateCsvMapper.toCsv(
                List.of(row),
                SalesStatisticsReportCsv.class,
                new SalesReportColumnOrder()
        );
        String text = new String(csv, StandardCharsets.UTF_8);

        String[] lines = text.split(System.lineSeparator());
        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("Tour name,Destination country,Destination city"));
        assertTrue(lines[1].contains("City Tour,France,Paris"));
        assertTrue(lines[1].matches(".*\\+12[.,]34%.*"));
        assertTrue(lines[1].matches(".*-2[.,]50%.*"));
        assertTrue(lines[1].matches(".*\\+25[.,]00%.*"));
    }

    @Test
    void test_toCsv_writesNAForNullValues_whenFieldsAreNull() {
        SalesStatisticsReportCsv row = SalesStatisticsReportCsv.builder()
                .tourName("City Tour")
                .destinationCountry("France")
                .destinationCity("Paris")
                .reportStartDate(LocalDate.of(2026, 6, 27))
                .reportEndDate(LocalDate.of(2026, 7, 4))
                .toursSold(10)
                .tourSoldDelta(null)
                .feedbackRateAvg(null)
                .feedbackRateMin(0)
                .feedbackRateAvgDelta(null)
                .finishedToursRevenue(null)
                .finishedToursRevenueDelta(null)
                .build();

        byte[] csv = ReportAggregateCsvMapper.toCsv(
                List.of(row),
                SalesStatisticsReportCsv.class,
                new SalesReportColumnOrder()
        );
        String text = new String(csv, StandardCharsets.UTF_8);

        assertTrue(text.contains("N/A"));
    }

    @Test
    void test_toCsv_throwsExportException_whenColumnOrderIsNull() {
        SalesStatisticsReportCsv row = SalesStatisticsReportCsv.builder()
                .tourName("City Tour")
                .destinationCountry("France")
                .destinationCity("Paris")
                .reportStartDate(LocalDate.of(2026, 6, 27))
                .reportEndDate(LocalDate.of(2026, 7, 4))
                .toursSold(10)
                .build();

        ExportException ex = assertThrows(
                ExportException.class,
                () -> ReportAggregateCsvMapper.toCsv(List.of(row), SalesStatisticsReportCsv.class, null)
        );

        assertEquals("CSV generation failed", ex.getMessage());
    }

    @Test
    void test_toSalesStatisticsReportList_mapsAllFields_whenSalesReportsProvided() {
        SalesAggregateReport report = new SalesAggregateReport();
        report.setTourName("City Tour");
        report.setDestinationCountry("France");
        report.setDestinationCity("Paris");
        report.setReportStartDate(LocalDate.of(2026, 6, 27));
        report.setReportEndDate(LocalDate.of(2026, 7, 4));
        report.setToursSold(10);
        report.setTourSoldDelta(BigDecimal.valueOf(10));
        report.setFeedbackRateAvg(BigDecimal.valueOf(4.0));
        report.setFeedbackRateMin(3);
        report.setFeedbackRateAvgDelta(BigDecimal.valueOf(5));
        report.setFinishedToursRevenue(BigDecimal.valueOf(1000));
        report.setFinishedToursRevenueDelta(BigDecimal.valueOf(20));

        List<SalesStatisticsReportCsv> result = ReportAggregateCsvMapper.toSalesStatisticsReportList(List.of(report));

        assertEquals(1, result.size());
        String csv = new String(
                ReportAggregateCsvMapper.toCsv(result, SalesStatisticsReportCsv.class, new SalesReportColumnOrder()),
                StandardCharsets.UTF_8
        );
        assertTrue(csv.contains("City Tour,France,Paris"));
    }

    @Test
    void test_toTravelAgentPerformanceList_mapsAllFields_whenPerformanceReportsProvided() {
        PerformanceAggregateReport report = new PerformanceAggregateReport();
        report.setTravelAgentName("Agent One");
        report.setTravelAgentEmail("agent@example.com");
        report.setReportStartDate(LocalDate.of(2026, 6, 27));
        report.setReportEndDate(LocalDate.of(2026, 7, 4));
        report.setToursSold(10);
        report.setTourSoldDelta(BigDecimal.valueOf(10));
        report.setFeedbackRateAvg(BigDecimal.valueOf(4.0));
        report.setFeedbackRateMin(3);
        report.setFeedbackRateAvgDelta(BigDecimal.valueOf(5));
        report.setFinishedToursRevenue(BigDecimal.valueOf(1000));
        report.setFinishedToursRevenueDelta(BigDecimal.valueOf(20));

        List<TravelAgentPerformanceReportCsv> result = ReportAggregateCsvMapper.toTravelAgentPerformanceList(List.of(report));

        assertEquals(1, result.size());
        String csv = new String(
                ReportAggregateCsvMapper.toCsv(result, TravelAgentPerformanceReportCsv.class, new PerformanceReportColumnOrder()),
                StandardCharsets.UTF_8
        );
        assertTrue(csv.contains("Agent One,agent@example.com"));
    }
}

