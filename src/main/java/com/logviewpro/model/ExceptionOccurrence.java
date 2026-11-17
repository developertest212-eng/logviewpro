package com.logviewpro.model;

import java.time.LocalDateTime;

public class ExceptionOccurrence {
    private final long lineNumber;
    private final LocalDateTime timestamp;
    private final String thread;
    private final String message;
    private final String fullStackTrace;
    private final String logFilePath;

    public ExceptionOccurrence(long lineNumber, LocalDateTime timestamp, String thread,
                               String message, String fullStackTrace, String logFilePath) {
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
        this.thread = thread;
        this.message = message;
        this.fullStackTrace = fullStackTrace;
        this.logFilePath = logFilePath;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getThread() {
        return thread;
    }

    public String getMessage() {
        return message;
    }

    public String getFullStackTrace() {
        return fullStackTrace;
    }

    public String getLogFilePath() {
        return logFilePath;
    }
}
