package com.epam.edp.demo.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SalesAggregateReport.class, name = "SALES"),
        @JsonSubTypes.Type(value = PerformanceAggregateReport.class, name = "PERFORMANCE")
})
public abstract class AggregateReport {
    protected LocalDate reportStartDate;
    protected LocalDate reportEndDate;
    protected int toursSold;
    protected BigDecimal tourSoldDelta;
    protected BigDecimal feedbackRateAvg;
    protected int feedbackRateMin;
    protected BigDecimal feedbackRateAvgDelta;
    protected BigDecimal finishedToursRevenue;
    protected BigDecimal finishedToursRevenueDelta;
}
