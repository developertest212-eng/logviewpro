package com.logviewpro.service;

import com.logviewpro.model.LogAnalyticsData;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.util.StacktraceUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public class LogAnalyticsService {

    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public LogAnalyticsData computeAnalytics(Collection<LogEntry> entries,
                                             LocalDateTime fromDate,
                                             LocalDateTime toDate) {
        LogAnalyticsData data = new LogAnalyticsData();
        if (entries == null || entries.isEmpty()) {
            return data;
        }

        long count = 0;
        long errors = 0;
        long warnings = 0;
        long info = 0;
        long debug = 0;
        long trace = 0;
        long fatal = 0;

        for (LogEntry entry : entries) {
            if (entry == null) continue;

            LocalDateTime ts = entry.getTimestamp();
            if (fromDate != null && ts != null && ts.isBefore(fromDate)) continue;
            if (toDate != null && ts != null && ts.isAfter(toDate)) continue;

            count++;

            // Timestamps
            if (ts != null) {
                if (data.getFirstTimestamp() == null || ts.isBefore(data.getFirstTimestamp())) {
                    data.setFirstTimestamp(ts);
                }
                if (data.getLastTimestamp() == null || ts.isAfter(data.getLastTimestamp())) {
                    data.setLastTimestamp(ts);
                }

                // Logs per minute
                String minuteKey = ts.format(MINUTE_FORMATTER);
                data.getLogsPerMinute().merge(minuteKey, 1L, Long::sum);

                // Logs by hour of day
                int hour = ts.getHour();
                data.getLogsByHourOfDay().merge(hour, 1L, Long::sum);

                // Logs per day
                LocalDate day = ts.toLocalDate();
                data.getLogsPerDay().merge(day, 1L, Long::sum);
            }

            // Levels
            LogLevel level = entry.getLevel();
            switch (level) {
                case ERROR -> {
                    errors++;
                    recordError(data, entry);
                }
                case FATAL -> {
                    fatal++;
                    recordError(data, entry);
                }
                case WARN -> warnings++;
                case INFO -> info++;
                case DEBUG -> debug++;
                case TRACE -> trace++;
                default -> {}
            }

            // Active threads
            String thread = entry.getThread();
            if (thread != null && !thread.isBlank()) {
                data.getTopThreads().merge(thread, 1L, Long::sum);
            }

            // Active loggers
            String logger = entry.getLogger();
            if (logger != null && !logger.isBlank()) {
                data.getTopClasses().merge(logger, 1L, Long::sum);
            }
        }

        data.setTotalEntries(count);
        data.setTotalErrors(errors);
        data.setTotalWarnings(warnings);
        data.setTotalInfo(info);
        data.setTotalDebug(debug);
        data.setTotalTrace(trace);
        data.setTotalFatal(fatal);

        return data;
    }

    private void recordError(LogAnalyticsData data, LogEntry entry) {
        String msg = entry.getMessage();
        if (msg != null && !msg.isBlank()) {
            // Trim long error message for grouping
            String shortMsg = msg.length() > 80 ? msg.substring(0, 80) + "..." : msg;
            data.getTopErrorMessages().merge(shortMsg, 1L, Long::sum);
            data.setLastDetectedError(shortMsg);
        }

        // Error trend by minute
        if (entry.getTimestamp() != null) {
            String minuteKey = entry.getTimestamp().format(MINUTE_FORMATTER);
            data.getErrorTrends().merge(minuteKey, 1L, Long::sum);
        }

        // Logger that produced the error
        if (entry.getLogger() != null && !entry.getLogger().isBlank()) {
            data.getTopErrorProducingLoggers().merge(entry.getLogger(), 1L, Long::sum);
        }

        // Exception class
        String exClass = StacktraceUtil.extractExceptionClass(entry.hasStackTrace() ? entry.getStackTrace() : entry.getMessage());
        if (exClass != null) {
            int lastDot = exClass.lastIndexOf('.');
            String simple = lastDot >= 0 ? exClass.substring(lastDot + 1) : exClass;
            data.getTopExceptions().merge(simple, 1L, Long::sum);
        }
    }
}
