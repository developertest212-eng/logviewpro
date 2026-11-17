package com.logviewpro.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

public class DateTimeUtil {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            // 2026-11-17 01:15:23.456 or 2026-11-17 01:15:23,456
            new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.MILLI_OF_SECOND, 1, 9, false).optionalEnd()
                    .optionalStart().appendLiteral(',').appendFraction(ChronoField.MILLI_OF_SECOND, 1, 9, false).optionalEnd()
                    .toFormatter(),
            // ISO-8601: 2026-11-17T01:15:23.456
            new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.MILLI_OF_SECOND, 1, 9, false).optionalEnd()
                    .optionalStart().appendPattern("XXX").optionalEnd()
                    .optionalStart().appendPattern("X").optionalEnd()
                    .toFormatter(),
            // 2026/11/17 01:15:23
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            // 17-Nov-2026 01:15:23.456
            new DateTimeFormatterBuilder().appendPattern("dd-MMM-yyyy HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.MILLI_OF_SECOND, 1, 9, false).optionalEnd()
                    .toFormatter(),
            // 17/11/2026 01:15:23
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            // Short time 01:15:23.456
            new DateTimeFormatterBuilder().appendPattern("HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.MILLI_OF_SECOND, 1, 9, false).optionalEnd()
                    .parseDefaulting(ChronoField.YEAR, 2026)
                    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                    .toFormatter()
    );

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter SHORT_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static LocalDateTime parseTimestamp(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.trim();
        // Remove trailing timezone name or offset if attached, e.g., "Z"
        if (cleaned.endsWith("Z")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(cleaned, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static String format(LocalDateTime ldt) {
        if (ldt == null) return "";
        return ldt.format(DISPLAY_FORMATTER);
    }

    public static String formatShort(LocalDateTime ldt) {
        if (ldt == null) return "";
        return ldt.format(SHORT_DISPLAY_FORMATTER);
    }
}
