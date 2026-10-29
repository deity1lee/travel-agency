package com.epam.edp.demo.export;

import com.opencsv.bean.AbstractBeanField;

import java.math.BigDecimal;
import java.util.Locale;

public class PercentageConverter extends AbstractBeanField<BigDecimal, String> {

    @Override
    protected BigDecimal convert(String value) {
        if (value == null
                || value.isBlank()
                || "N/A".equals(value)) {
            return null;
        }
        return new BigDecimal(value.replace("%", ""));
    }

    @Override
    public String convertToWrite(Object value) {
        if (value == null) {
            return "N/A";
        }
        BigDecimal pct = (BigDecimal) value;
        return String.format(Locale.US, "%+.2f%%", pct);
    }
}
