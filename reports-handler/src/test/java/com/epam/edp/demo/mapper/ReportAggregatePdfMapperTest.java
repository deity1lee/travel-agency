package com.epam.edp.demo.mapper;

import com.epam.edp.demo.exception.ExportException;
import com.epam.edp.demo.model.SalesAggregateReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportAggregatePdfMapperTest {

    @Test
    void test_toPdf_returnsEmptyByteArray_whenReportIsNull() {
        byte[] pdf = ReportAggregatePdfMapper.toPdf(null, SalesAggregateReport.class);

        assertEquals(0, pdf.length);
    }

    @Test
    void test_toPdf_returnsEmptyByteArray_whenReportIsEmpty() {
        byte[] pdf = ReportAggregatePdfMapper.toPdf(List.of(), SalesAggregateReport.class);

        assertEquals(0, pdf.length);
    }

    @Test
    void test_toPdf_generatesPdfBytes_whenReportHasData() {
        SalesAggregateReport row = new SalesAggregateReport();
        row.setTourName("City Tour");
        row.setDestinationCountry("France");
        row.setDestinationCity("Paris");
        row.setReportStartDate(LocalDate.of(2026, 6, 27));
        row.setReportEndDate(LocalDate.of(2026, 7, 4));
        row.setToursSold(10);
        row.setTourSoldDelta(BigDecimal.valueOf(10));
        row.setFeedbackRateAvg(BigDecimal.valueOf(4.0));
        row.setFeedbackRateMin(3);
        row.setFeedbackRateAvgDelta(BigDecimal.valueOf(5));
        row.setFinishedToursRevenue(BigDecimal.valueOf(1000));
        row.setFinishedToursRevenueDelta(BigDecimal.valueOf(20));

        byte[] pdf = ReportAggregatePdfMapper.toPdf(List.of(row), SalesAggregateReport.class);
        String prefix = new String(pdf, 0, 4, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", prefix);
    }

    @Test
    void test_toPdf_generatesPdfBytesWithNullFields_whenReportContainsNullValues() {
        SalesAggregateReport row = new SalesAggregateReport();
        row.setTourName("City Tour");
        row.setDestinationCountry(null);
        row.setDestinationCity(null);
        row.setReportStartDate(null);
        row.setReportEndDate(null);
        row.setToursSold(10);
        row.setTourSoldDelta(null);
        row.setFeedbackRateAvg(null);
        row.setFeedbackRateMin(0);
        row.setFeedbackRateAvgDelta(null);
        row.setFinishedToursRevenue(null);
        row.setFinishedToursRevenueDelta(null);

        byte[] pdf = ReportAggregatePdfMapper.toPdf(List.of(row), SalesAggregateReport.class);
        String prefix = new String(pdf, 0, 4, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", prefix);
    }

    @Test
    void test_toPdf_throwsExportException_whenClazzIsNull() {
        SalesAggregateReport row = new SalesAggregateReport();
        row.setTourName("City Tour");

        ExportException ex = assertThrows(
                ExportException.class,
                () -> ReportAggregatePdfMapper.toPdf(List.of(row), null)
        );

        assertEquals("Failed to generate PDF", ex.getMessage());
    }
}

