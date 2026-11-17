package com.logviewpro.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.time.LocalDateTime;
import java.util.Objects;

public class LogEntry {
    private long id;
    private long lineNumber;
    private LocalDateTime timestamp;
    private String rawTimestamp;
    private LogLevel level = LogLevel.UNKNOWN;
    private String thread;
    private String logger;
    private String message;
    private String stackTrace;
    private String rawText;
    private String filePath;
    private long byteOffset;
    private int byteLength;

    private final BooleanProperty bookmarked = new SimpleBooleanProperty(false);
    private final BooleanProperty highlighted = new SimpleBooleanProperty(false);

    public LogEntry() {}

    public LogEntry(long id, long lineNumber, LocalDateTime timestamp, String rawTimestamp,
                    LogLevel level, String thread, String logger, String message,
                    String stackTrace, String rawText, String filePath) {
        this.id = id;
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
        this.rawTimestamp = rawTimestamp;
        this.level = level != null ? level : LogLevel.UNKNOWN;
        this.thread = thread != null ? thread : "";
        this.logger = logger != null ? logger : "";
        this.message = message != null ? message : "";
        this.stackTrace = stackTrace != null ? stackTrace : "";
        this.rawText = rawText != null ? rawText : "";
        this.filePath = filePath != null ? filePath : "";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRawTimestamp() {
        return rawTimestamp;
    }

    public void setRawTimestamp(String rawTimestamp) {
        this.rawTimestamp = rawTimestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level != null ? level : LogLevel.UNKNOWN;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace != null ? stackTrace : "";
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace != null ? stackTrace : "";
    }

    public boolean hasStackTrace() {
        return stackTrace != null && !stackTrace.trim().isEmpty();
    }

    public String getRawText() {
        return rawText != null ? rawText : "";
    }

    public void setRawText(String rawText) {
        this.rawText = rawText != null ? rawText : "";
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getByteOffset() {
        return byteOffset;
    }

    public void setByteOffset(long byteOffset) {
        this.byteOffset = byteOffset;
    }

    public int getByteLength() {
        return byteLength;
    }

    public void setByteLength(int byteLength) {
        this.byteLength = byteLength;
    }

    public boolean isBookmarked() {
        return bookmarked.get();
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked.set(bookmarked);
    }

    public BooleanProperty bookmarkedProperty() {
        return bookmarked;
    }

    public boolean isHighlighted() {
        return highlighted.get();
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted.set(highlighted);
    }

    public BooleanProperty highlightedProperty() {
        return highlighted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return lineNumber == logEntry.lineNumber &&
               Objects.equals(filePath, logEntry.filePath) &&
               Objects.equals(rawText, logEntry.rawText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, lineNumber, rawText);
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s] %s - %s",
                rawTimestamp != null ? rawTimestamp : (timestamp != null ? timestamp.toString() : ""),
                level,
                thread,
                logger,
                message);
    }
}
