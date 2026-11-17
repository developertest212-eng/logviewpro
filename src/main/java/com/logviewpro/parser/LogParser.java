package com.logviewpro.parser;

import com.logviewpro.model.LogEntry;

public interface LogParser {
    /**
     * Checks whether this line represents the beginning of a new log entry.
     */
    boolean isNewEntry(String line);

    /**
     * Parses a single header line into a LogEntry.
     */
    LogEntry parseHeader(String line, long lineNumber, String filePath);

    /**
     * Attaches subsequent continuation lines (such as stack trace or multi-line message) to the entry.
     */
    void appendContinuation(LogEntry entry, String line);
}
