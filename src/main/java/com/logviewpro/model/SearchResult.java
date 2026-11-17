package com.logviewpro.model;

public class SearchResult {
    private final String filePath;
    private final long lineNumber;
    private final LogEntry logEntry;
    private final int matchStartIndex;
    private final int matchLength;
    private final String snippet;

    public SearchResult(String filePath, long lineNumber, LogEntry logEntry,
                        int matchStartIndex, int matchLength, String snippet) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.logEntry = logEntry;
        this.matchStartIndex = matchStartIndex;
        this.matchLength = matchLength;
        this.snippet = snippet;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public int getMatchStartIndex() {
        return matchStartIndex;
    }

    public int getMatchLength() {
        return matchLength;
    }

    public String getSnippet() {
        return snippet;
    }

    @Override
    public String toString() {
        return String.format("[%s : Line %d] %s", filePath != null ? filePath : "", lineNumber, snippet);
    }
}
