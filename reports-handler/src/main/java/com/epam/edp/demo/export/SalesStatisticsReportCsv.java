package com.epam.edp.demo.export;

import com.opencsv.bean.CsvBindByName;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class SalesStatisticsReportCsv extends CsvReport {

    @CsvBindByName(column = "Tour name")
    private String tourName;

    @CsvBindByName(column = "Destination country")
    private String destinationCountry;

    @CsvBindByName(column = "Destination city")
    private String destinationCity;
}
