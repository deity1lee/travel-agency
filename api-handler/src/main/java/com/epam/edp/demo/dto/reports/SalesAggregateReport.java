package com.epam.edp.demo.dto.reports;

import lombok.Data;

@Data
public class SalesAggregateReport extends AggregateReport {
    private String tourName;
    private String destinationCountry;
    private String destinationCity;
}
