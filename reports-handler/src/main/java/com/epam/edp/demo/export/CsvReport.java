package com.epam.edp.demo.export;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import com.opencsv.bean.CsvNumber;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@SuperBuilder
public abstract class CsvReport {
    @CsvBindByName(column = "Report period start")
    @CsvDate("dd-MM-yyyy")
    protected LocalDate reportStartDate;

    @CsvBindByName(column = "Report period end")
    @CsvDate("dd-MM-yyyy")
    protected LocalDate reportEndDate;

    @CsvBindByName(column = "Tours sold")
    protected int toursSold;

    @CsvCustomBindByName(
            column = "Delta of tours sold to previous period %",
            converter = PercentageConverter.class
    )
    protected BigDecimal tourSoldDelta;

    @CsvBindByName(column = "Average Feedback rate for Travel experience (1 to 5)")
    @CsvNumber("0.00")
    protected BigDecimal feedbackRateAvg;

    @CsvBindByName(column = "Minimum Feedback rate for Travel experience (1 to 5)")
    protected int feedbackRateMin;

    @CsvCustomBindByName(
            column = "Delta Average Feedback rate for Travel experience to previous period %",
            converter = PercentageConverter.class
    )
    protected BigDecimal feedbackRateAvgDelta;

    @CsvBindByName(column = "Revenue for tours finished within reported period (USD)")
    @CsvNumber("0.00")
    protected BigDecimal finishedToursRevenue;

    @CsvCustomBindByName(
            column = "Delta for revenue for tours finished to previous period %",
            converter = PercentageConverter.class
    )
    protected BigDecimal finishedToursRevenueDelta;
}
