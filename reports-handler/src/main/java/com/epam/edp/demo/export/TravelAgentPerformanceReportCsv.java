package com.epam.edp.demo.export;

import com.opencsv.bean.CsvBindByName;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class TravelAgentPerformanceReportCsv extends CsvReport {

    @CsvBindByName(column = "Travel Agent")
    private String travelAgentName;

    @CsvBindByName(column = "TA e-mail")
    private String travelAgentEmail;
}
