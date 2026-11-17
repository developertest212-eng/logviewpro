package com.logviewpro.model;

import java.time.LocalDateTime;

public class AlertNotification {
    private long id;
    private String ruleName;
    private String title;
    private String message;
    private LocalDateTime timestamp;
    private String sourceFile;
    private long lineNumber;
    private boolean read;

    public AlertNotification() {
        this.timestamp = LocalDateTime.now();
    }

    public AlertNotification(String ruleName, String title, String message, String sourceFile, long lineNumber) {
        this.ruleName = ruleName;
        this.title = title;
        this.message = message;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
