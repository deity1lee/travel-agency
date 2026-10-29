package com.epam.edp.demo.mapper;

import com.epam.edp.demo.exception.ExportException;
import com.epam.edp.demo.export.PageNumberEvent;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ReportAggregatePdfMapper {

    private ReportAggregatePdfMapper() {}

    public static <T> byte[] toPdf(List<T> report, Class<T> clazz) {
        if (report == null || report.isEmpty()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumberEvent());
            document.open();

            document.addTitle(clazz.getSimpleName() + " Report");
            document.addAuthor("Travel Agency App");
            document.addCreationDate();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);

            Paragraph title = new Paragraph(clazz.getSimpleName() + " Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);

            document.add(title);

            List<Field> fields = MapperUtils.getAllFields(clazz);
            PdfPTable table = new PdfPTable(fields.size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);

            addHeadersToTable(fields, table);
            addCells(report, fields, table);

            document.add(table);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.warn("caught exception when exporting pdf", e);
            throw new ExportException("Failed to generate PDF");
        }
    }

    private static void addHeadersToTable(List<Field> fields, PdfPTable table) {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);

        for (Field field : fields) {
            PdfPCell header = new PdfPCell(new Phrase(prettify(field.getName()), headerFont));
            header.setBackgroundColor(new Color(52, 73, 94));
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setPadding(8);

            table.addCell(header);
        }
    }

    private static <T> void addCells(List<T> report, List<Field> fields, PdfPTable table) throws IllegalAccessException {
        Color even = new Color(215, 215, 215);
        Color odd = Color.WHITE;

        boolean evenRow = false;

        Font normalFont = new Font(Font.HELVETICA, 10);

        for (T item : report) {
            Color rowColor = evenRow ? even : odd;

            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(item);

                String text;

                if (value != null) {
                    if (value instanceof LocalDate d) {
                        text = d.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                    } else if (value instanceof BigDecimal bd) {
                        text = NumberFormat.getNumberInstance().format(bd);
                        if (field.getName().contains("Delta")) {
                            if (bd.compareTo(BigDecimal.ZERO) > 0){
                                text = "+" + text;
                            }
                            text = text + "%";
                        }
                    } else {
                        text = String.valueOf(value);
                    }
                } else {
                    text = "N/A";
                }

                PdfPCell cell = new PdfPCell(new Phrase(text, normalFont));
                cell.setBackgroundColor(rowColor);
                cell.setPadding(6);

                table.addCell(cell);
            }

            evenRow = !evenRow;
        }
    }

    private static String prettify(String name) {
        return Arrays.stream(name.split("(?=[A-Z])"))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }
}
