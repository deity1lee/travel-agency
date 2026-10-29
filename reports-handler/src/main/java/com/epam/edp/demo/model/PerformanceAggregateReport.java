package com.epam.edp.demo.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PerformanceAggregateReport extends AggregateReport {
    private String travelAgentName;
    private String travelAgentEmail;
}
