package com.logviewpro.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class LogFileMeta {
    private String filePath;
    private String fileName;
    private long fileSizeBytes;
    private long lineCount;
    private String encoding = "UTF-8";
    private LocalDateTime lastModified;
    private boolean isRotated;
    private final BooleanProperty pinned = new SimpleBooleanProperty(false);
    private final BooleanProperty monitored = new SimpleBooleanProperty(false);
    private long errorCount;
    private long warnCount;
    private long fatalCount;
    private long infoCount;
    private long debugCount;
    private long traceCount;
    private LocalDateTime firstTimestamp;
    private LocalDateTime lastTimestamp;

    public LogFileMeta() {}

    public LogFileMeta(File file) {
        if (file != null && file.exists()) {
            this.filePath = file.getAbsolutePath();
            this.fileName = file.getName();
            this.fileSizeBytes = file.length();
            this.lastModified = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(file.lastModified()),
                    ZoneId.systemDefault()
            );
            this.isRotated = detectIfRotated(file.getName());
        }
    }

    private static boolean detectIfRotated(String name) {
        if (name == null) return false;
        // Patterns like app.log.1, app.log.2026-11-16, app-1.log, app.log.gz
        return name.matches(".*\\.(log|txt|out)\\.[0-9]+.*") ||
               name.matches(".*[._][0-9]{4}-[0-9]{2}-[0-9]{2}.*") ||
               name.matches(".*\\.(gz|zip)$");
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFormattedSize() {
        if (fileSizeBytes < 1024) return fileSizeBytes + " B";
        int exp = (int) (Math.log(fileSizeBytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", fileSizeBytes / Math.pow(1024, exp), pre);
    }

    public long getLineCount() {
        return lineCount;
    }

    public void setLineCount(long lineCount) {
        this.lineCount = lineCount;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isRotated() {
        return isRotated;
    }

    public void setRotated(boolean rotated) {
        isRotated = rotated;
    }

    public boolean isPinned() {
        return pinned.get();
    }

    public void setPinned(boolean pinned) {
        this.pinned.set(pinned);
    }

    public BooleanProperty pinnedProperty() {
        return pinned;
    }

    public boolean isMonitored() {
        return monitored.get();
    }

    public void setMonitored(boolean monitored) {
        this.monitored.set(monitored);
    }

    public BooleanProperty monitoredProperty() {
        return monitored;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getWarnCount() {
        return warnCount;
    }

    public void setWarnCount(long warnCount) {
        this.warnCount = warnCount;
    }

    public long getFatalCount() {
        return fatalCount;
    }

    public void setFatalCount(long fatalCount) {
        this.fatalCount = fatalCount;
    }

    public long getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(long infoCount) {
        this.infoCount = infoCount;
    }

    public long getDebugCount() {
        return debugCount;
    }

    public void setDebugCount(long debugCount) {
        this.debugCount = debugCount;
    }

    public long getTraceCount() {
        return traceCount;
    }

    public void setTraceCount(long traceCount) {
        this.traceCount = traceCount;
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
}
