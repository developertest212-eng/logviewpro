package com.logviewpro.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class LogAnalyticsData {
    private long totalEntries;
    private long totalErrors;
    private long totalWarnings;
    private long totalInfo;
    private long totalDebug;
    private long totalTrace;
    private long totalFatal;

    private double errorPercentage;
    private double warningPercentage;

    private LocalDateTime firstTimestamp;
    private LocalDateTime lastTimestamp;
    private String lastDetectedError;

    // Time-based stats
    private final Map<String, Long> logsPerMinute = new TreeMap<>();
    private final Map<Integer, Long> logsByHourOfDay = new TreeMap<>(); // 0 to 23
    private final Map<LocalDate, Long> logsPerDay = new TreeMap<>();
    private final Map<String, Long> errorTrends = new TreeMap<>();

    // Frequency rankings
    private final Map<String, Long> topExceptions = new HashMap<>();
    private final Map<String, Long> topErrorMessages = new HashMap<>();
    private final Map<String, Long> topThreads = new HashMap<>();
    private final Map<String, Long> topClasses = new HashMap<>();
    private final Map<String, Long> topErrorProducingLoggers = new HashMap<>();

    public LogAnalyticsData() {
        for (int i = 0; i < 24; i++) {
            logsByHourOfDay.put(i, 0L);
        }
    }

    public long getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(long totalEntries) {
        this.totalEntries = totalEntries;
        calculatePercentages();
    }

    public long getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(long totalErrors) {
        this.totalErrors = totalErrors;
        calculatePercentages();
    }

    public long getTotalWarnings() {
        return totalWarnings;
    }

    public void setTotalWarnings(long totalWarnings) {
        this.totalWarnings = totalWarnings;
        calculatePercentages();
    }

    public long getTotalInfo() {
        return totalInfo;
    }

    public void setTotalInfo(long totalInfo) {
        this.totalInfo = totalInfo;
    }

    public long getTotalDebug() {
        return totalDebug;
    }

    public void setTotalDebug(long totalDebug) {
        this.totalDebug = totalDebug;
    }

    public long getTotalTrace() {
        return totalTrace;
    }

    public void setTotalTrace(long totalTrace) {
        this.totalTrace = totalTrace;
    }

    public long getTotalFatal() {
        return totalFatal;
    }

    public void setTotalFatal(long totalFatal) {
        this.totalFatal = totalFatal;
        calculatePercentages();
    }

    private void calculatePercentages() {
        if (totalEntries > 0) {
            this.errorPercentage = ((double) (totalErrors + totalFatal) / totalEntries) * 100.0;
            this.warningPercentage = ((double) totalWarnings / totalEntries) * 100.0;
        } else {
            this.errorPercentage = 0.0;
            this.warningPercentage = 0.0;
        }
    }

    public double getErrorPercentage() {
        return errorPercentage;
    }

    public double getWarningPercentage() {
        return warningPercentage;
    }

    public LocalDateTime getFirstTimestamp() {
        return firstTimestamp;
    }

    public void setFirstTimestamp(LocalDateTime firstTimestamp) {
        this.firstTimestamp = firstTimestamp;
    }

    public LocalDateTime getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(LocalDateTime lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public String getLastDetectedError() {
        return lastDetectedError;
    }

    public void setLastDetectedError(String lastDetectedError) {
        this.lastDetectedError = lastDetectedError;
    }

    public Map<String, Long> getLogsPerMinute() {
        return logsPerMinute;
    }

    public Map<Integer, Long> getLogsByHourOfDay() {
        return logsByHourOfDay;
    }

    public Map<LocalDate, Long> getLogsPerDay() {
        return logsPerDay;
    }

    public Map<String, Long> getErrorTrends() {
        return errorTrends;
    }

    public Map<String, Long> getTopExceptions() {
        return topExceptions;
    }

    public Map<String, Long> getTopErrorMessages() {
        return topErrorMessages;
    }

    public Map<String, Long> getTopThreads() {
        return topThreads;
    }

    public Map<String, Long> getTopClasses() {
        return topClasses;
    }

    public Map<String, Long> getTopErrorProducingLoggers() {
        return topErrorProducingLoggers;
    }

    public static List<Map.Entry<String, Long>> getTopN(Map<String, Long> map, int n) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .toList();
    }
}
