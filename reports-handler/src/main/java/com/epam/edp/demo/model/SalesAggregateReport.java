package com.epam.edp.demo.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SalesAggregateReport extends AggregateReport {
    private String tourName;
    private String destinationCountry;
    private String destinationCity;
}
