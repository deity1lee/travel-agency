package com.epam.edp.demo.validation.tour;

import com.epam.edp.demo.dto.tour.TourRequestDTO;
import com.epam.edp.demo.entity.tourModel.DurationBucket;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.entity.tourModel.TourSortBy;
import com.epam.edp.demo.entity.tourModel.TourType;
import com.epam.edp.demo.exception.tour.TourValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TourSearchValidator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 6;

    public static TourRequestDTO validateAndBuild(Map<String, String> params,
                                           MultiValueMap<String, String> multiValueParams) {
        Map<String, String> errors = new LinkedHashMap<>();

        String destination = getParam(params, "destination");
        List<DurationBucket> durations = getDurationListParam(params, multiValueParams, errors);
        DateRangeParser.DateRange startDateRange = getDateRangeParam(params, errors);
        LocalDate startDateFrom = startDateRange == null ? null : startDateRange.getFrom();
        LocalDate startDateTo = startDateRange == null ? null : startDateRange.getTo();
        Integer adults = firstIntegerParam(params, errors, "guestQuantity.adults", "guestQuantity[adults]", "adults");
        Integer children = firstIntegerParam(params, errors, "guestQuantity.children", "guestQuantity[children]", "children");

        Map<String, Integer> parsedGuestQuantity = parseGuestQuantity(params, errors);
        if (adults == null) {
            adults = parsedGuestQuantity.get("adults");
        }
        if (children == null) {
            children = parsedGuestQuantity.get("children");
        }

        validateGuestQuantity(adults, children, errors);

        int page = getPositiveIntegerOrDefault(params, "page", DEFAULT_PAGE, errors);
        int pageSize = getPositiveIntegerOrDefault(params, "pageSize", DEFAULT_PAGE_SIZE, errors);

        List<MealPlan> mealPlans = getEnumListParam(params, multiValueParams, "mealPlan", MealPlan.class, errors);
        List<TourType> tourTypes = getEnumListParam(params, multiValueParams, "tourType", TourType.class, errors);

        TourSortBy sortBy = getEnumParam(params, "sortBy", TourSortBy.class, errors);

        if (!errors.isEmpty()) {
            throw new TourValidationException(errors);
        }

        return new TourRequestDTO(destination, startDateFrom, startDateTo, durations, adults,
                children, mealPlans, tourTypes, sortBy, page, pageSize);
    }

    private static List<DurationBucket> getDurationListParam(Map<String, String> params,
                                                      Map<String, List<String>> multiValueParams,
                                                      Map<String, String> errors) {
        List<String> rawValues = collectRawValues(params, multiValueParams, "duration");
        if (rawValues.isEmpty()) {
            return List.of();
        }
        List<DurationBucket> result = new ArrayList<>();
        for (String raw : rawValues) {
            if (isBlank(raw)) {
                continue;
            }
            Optional<DurationBucket> bucket = DurationBucket.fromWireValue(raw);
            if (bucket.isEmpty()) {
                errors.put("duration", "must be one of: " + Arrays.stream(DurationBucket.values())
                        .map(DurationBucket::getWireValue)
                        .collect(Collectors.joining(", ")));
                return List.of();
            }
            result.add(bucket.get());
        }
        return result;
    }

    private static void validateGuestQuantity(Integer adults, Integer children, Map<String, String> errors) {
        if (adults != null && adults < 1) {
            errors.put("guestQuantity.adults", "must be greater than or equal to 1");
        }
        if (children != null && children < 0) {
            errors.put("guestQuantity.children", "must be greater than or equal to 0");
        }
    }

    private static int getPositiveIntegerOrDefault(Map<String, String> params,
                                            String key, int defaultValue, Map<String, String> errors) {
        Integer value = getIntegerParam(params, key, errors);
        if (value == null) {
            return defaultValue;
        }
        if (value < 1) {
            errors.put(key, "must be greater than or equal to 1");
            return defaultValue;
        }
        return value;
    }

    private static DateRangeParser.DateRange getDateRangeParam(Map<String, String> params, Map<String, String> errors) {
        String value = getParam(params, "startDate");
        if (isBlank(value)) {
            return null;
        }
        DateRangeParser.Result result = DateRangeParser.parse(value.trim());
        if (result.isSuccess()) {
            return result.getDateRange();
        }
        if (result.getFailure() == DateRangeParser.Failure.FROM_AFTER_TO) {
            errors.put("startDate", "from-date must be on or before to-date");
        } else {
            errors.put("startDate", "must be in format \"MMM d yyyy - MMM d yyyy\"");
        }
        return null;
    }

    private static Integer firstIntegerParam(Map<String, String> params,
                                      Map<String, String> errors, String... keys) {
        for (String key : keys) {
            String value = getParam(params, key);
            if (!isBlank(value)) {
                return getIntegerParam(params, key, errors);
            }
        }
        return null;
    }

    private static Integer getIntegerParam(Map<String, String> params, String key, Map<String, String> errors) {
        String value = getParam(params, key);
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            errors.put(key, "must be a valid integer");
            return null;
        }
    }

    private static <T extends Enum<T>> T getEnumParam(Map<String, String> params,
                                               String key, Class<T> enumClass, Map<String, String> errors) {
        String value = getParam(params, key);
        if (isBlank(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.put(key, "must be one of: " + Arrays.toString(enumClass.getEnumConstants()));
            return null;
        }
    }

    private static <T extends Enum<T>> List<T> getEnumListParam(Map<String, String> params,
                                                        Map<String, List<String>> multiValueParams,
                                                        String key, Class<T> enumClass,
                                                        Map<String, String> errors) {
        List<String> rawValues = collectRawValues(params, multiValueParams, key);
        if (rawValues.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>();
        for (String raw : rawValues) {
            if (isBlank(raw)) {
                continue;
            }
            try {
                result.add(Enum.valueOf(enumClass, raw.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                errors.put(key, "must be one of: " + Arrays.toString(enumClass.getEnumConstants()));
                return List.of();
            }
        }
        return result;
    }

    private static List<String> collectRawValues(Map<String, String> params,
                                          Map<String, List<String>> multiValueParams,
                                          String key) {
        if (multiValueParams != null) {
            List<String> values = multiValueParams.get(key);
            if (values != null && !values.isEmpty()) {
                return values;
            }
        }
        String single = getParam(params, key);
        return isBlank(single) ? List.of() : List.of(single);
    }

    private static String getParam(Map<String, String> params, String key) {
        if (params == null) {
            return null;
        }
        return params.get(key);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static Map<String, Integer> parseGuestQuantity(Map<String, String> params, Map<String, String> errors) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String value = getParam(params, "guestQuantity");
        if (isBlank(value)) {
            return result;
        }
        try {
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(
                    value.trim(),
                    new TypeReference<>() {
                    }
            );
            putGuestQuantityValue(parsed, result, errors, "adults");
            putGuestQuantityValue(parsed, result, errors, "children");
        } catch (Exception e) {
            log.info("guestQuantity must be a valid JSON object: ", e);
            errors.put("guestQuantity", "must be a valid JSON object");
        }
        return result;
    }

    private static void putGuestQuantityValue(Map<String, Object> parsed,
                                       Map<String, Integer> result,
                                       Map<String, String> errors,
                                       String key) {
        Object value = parsed.get(key);
        if (value == null) {
            return;
        }
        if (value instanceof Number) {
            result.put(key, ((Number) value).intValue());
            return;
        }
        if (value instanceof String) {
            try {
                result.put(key, Integer.valueOf(((String) value).trim()));
                return;
            } catch (NumberFormatException e) {
                errors.put("guestQuantity." + key, "must be a valid integer");
                return;
            }
        }
        errors.put("guestQuantity." + key, "must be a valid integer");
    }
}
