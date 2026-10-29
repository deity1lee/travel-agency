package com.epam.edp.demo.mapper;

import com.epam.edp.demo.exception.ExportException;
import com.epam.edp.demo.export.CsvColumnOrder;
import com.epam.edp.demo.export.PercentageConverter;
import com.epam.edp.demo.model.PerformanceAggregateReport;
import com.epam.edp.demo.model.SalesAggregateReport;
import com.epam.edp.demo.export.SalesStatisticsReportCsv;
import com.epam.edp.demo.export.TravelAgentPerformanceReportCsv;
import com.opencsv.CSVWriter;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import lombok.extern.slf4j.Slf4j;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ReportAggregateCsvMapper {

    private ReportAggregateCsvMapper() {}

    public static <T> byte[] toCsv(List<T> report,
                                   Class<T> clazz,
                                   CsvColumnOrder columnOrder) {
        try (StringWriter writer = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(writer,
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.NO_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     System.lineSeparator())) {

            List<String> columns = columnOrder.columns();
            Map<String, Integer> orderMap = IntStream.range(0, columns.size())
                    .boxed()
                    .collect(Collectors.toMap(columns::get, Function.identity()));

            List<Field> fields = MapperUtils.getAllFields(clazz).stream()
                    .filter(f ->
                            f.isAnnotationPresent(CsvBindByName.class) ||
                                    f.isAnnotationPresent(CsvCustomBindByName.class))
                    .sorted(Comparator.comparingInt(f ->
                            orderMap.getOrDefault(getColumnName(f), Integer.MAX_VALUE)))
                    .toList();

            fields.forEach(f -> f.setAccessible(true));

            String[] header = fields.stream()
                    .map(ReportAggregateCsvMapper::getColumnName)
                    .toArray(String[]::new);

            csvWriter.writeNext(header);

            Map<Field, PercentageConverter> converterCache = new HashMap<>();

            for (T item : report) {
                String[] row = new String[fields.size()];

                for (int i = 0; i < fields.size(); i++) {
                    Field field = fields.get(i);

                    Object value = field.get(item);

                    row[i] = getRowValue(value, field, converterCache);
                }

                csvWriter.writeNext(row);
            }

            return writer.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("caught exception when exporting csv", e);
            throw new ExportException("CSV generation failed");
        }
    }

    private static String getRowValue(Object value,
                                      Field field,
                                      Map<Field, PercentageConverter> converterCache) {
        if (value == null) {
            return "N/A";
        }

        CsvCustomBindByName custom = field.getAnnotation(CsvCustomBindByName.class);
        if (custom == null) {
            return value.toString();
        }

        String row;
        try {
            PercentageConverter converter = converterCache.computeIfAbsent(field, f -> {
                try {
                    return (PercentageConverter) f.getAnnotation(CsvCustomBindByName.class)
                            .converter()
                            .getDeclaredConstructor()
                            .newInstance();
                } catch (Exception e) {
                    log.debug("Caught exception in csv generation. Setting value of field {} to null", field, e);
                    return null;
                }
            });

            converter.setField(field);
            converter.setType(field.getType());

            row = converter.convertToWrite(value);
        } catch (Exception e) {
            log.debug("Caught exception in csv generation. Setting value of field {} to {}", field, value, e);
            row = value.toString();
        }

        return row;
    }

    private static String getColumnName(Field field) {
        log.debug("reading annotation for field {}", field.getName());
        CsvBindByName bindByName = field.getAnnotation(CsvBindByName.class);
        if (bindByName != null) {
            return bindByName.column();
        }

        CsvCustomBindByName customBind = field.getAnnotation(CsvCustomBindByName.class);
        if (customBind != null) {
            return customBind.column();
        }

        throw new IllegalArgumentException(
                "Field '%s' is missing CSV binding annotation".formatted(field.getName()));
    }

    public static List<TravelAgentPerformanceReportCsv> toTravelAgentPerformanceList(List<PerformanceAggregateReport> reports) {
        return reports.stream()
                .map(ReportAggregateCsvMapper::toTravelAgentPerformance)
                .toList();
    }

    private static TravelAgentPerformanceReportCsv toTravelAgentPerformance(PerformanceAggregateReport report) {
        return TravelAgentPerformanceReportCsv.builder()
                .travelAgentName(report.getTravelAgentName())
                .travelAgentEmail(report.getTravelAgentEmail())
                .reportStartDate(report.getReportStartDate())
                .reportEndDate(report.getReportEndDate())
                .toursSold(report.getToursSold())
                .tourSoldDelta(report.getTourSoldDelta())
                .feedbackRateAvg(report.getFeedbackRateAvg())
                .feedbackRateMin(report.getFeedbackRateMin())
                .feedbackRateAvgDelta(report.getFeedbackRateAvgDelta())
                .finishedToursRevenue(report.getFinishedToursRevenue())
                .finishedToursRevenueDelta(report.getFinishedToursRevenueDelta())
                .build();
    }

    public static List<SalesStatisticsReportCsv> toSalesStatisticsReportList(List<SalesAggregateReport> reports) {
        return reports.stream()
                .map(ReportAggregateCsvMapper::toSalesStatisticsReport)
                .toList();
    }

    private static SalesStatisticsReportCsv toSalesStatisticsReport(SalesAggregateReport report) {
        return SalesStatisticsReportCsv.builder()
                .tourName(report.getTourName())
                .destinationCountry(report.getDestinationCountry())
                .destinationCity(report.getDestinationCity())
                .reportStartDate(report.getReportStartDate())
                .reportEndDate(report.getReportEndDate())
                .toursSold(report.getToursSold())
                .tourSoldDelta(report.getTourSoldDelta())
                .feedbackRateAvg(report.getFeedbackRateAvg())
                .feedbackRateMin(report.getFeedbackRateMin())
                .feedbackRateAvgDelta(report.getFeedbackRateAvgDelta())
                .finishedToursRevenue(report.getFinishedToursRevenue())
                .finishedToursRevenueDelta(report.getFinishedToursRevenueDelta())
                .build();
    }

}
