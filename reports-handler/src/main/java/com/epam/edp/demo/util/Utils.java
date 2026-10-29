package com.epam.edp.demo.util;

import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class Utils {

    private Utils(){}

    public static String[] getCityCountry(String destination) {
        String[] split = destination.split("\\s*,\\s*");

        String[] cityCountry = new String[] {
                split.length > 0 ? split[0] : "",
                split.length > 1 ? split[1] : ""
        };
        return cityCountry;
    }

    public static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static BigDecimal getLoss(BookingStatusChangedEvent dto) {
        switch (dto.bookingStatus()) {
            case CONFIRMED:
            case FINISHED:
                return BigDecimal.valueOf(0);
            case CANCELED:
                return BigDecimal.valueOf(dto.price() - dto.cancelCharge());
            default:
                log.info("encountered default status in get loss method. Please Refactor code");
                return BigDecimal.valueOf(0);
        }
    }

    public static BigDecimal getRevenue(BookingStatusChangedEvent dto) {
        switch (dto.bookingStatus()) {
            case CONFIRMED:
            case FINISHED:
                return BigDecimal.valueOf(dto.price());
            case CANCELED:
                return BigDecimal.valueOf(dto.cancelCharge());
            default:
                log.info("encountered default status in get revenue method. Please Refactor code");
                return BigDecimal.valueOf(0);
        }
    }

    public static BigDecimal delta(BigDecimal cur, BigDecimal prev) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0 || cur == null) {
            return null;
        }

        return cur.subtract(prev)
                .divide(prev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public static BigDecimal delta(Integer cur, Integer prev) {
        if (prev == null || prev == 0 || cur == null) {
            return null;
        }
        return delta(BigDecimal.valueOf(cur), BigDecimal.valueOf(prev));
    }
}
