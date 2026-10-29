package com.epam.edp.demo.dto.reports;

import lombok.Data;

@Data
public class PerformanceAggregateReport extends AggregateReport {
    private String travelAgentName;
    private String travelAgentEmail;
}
