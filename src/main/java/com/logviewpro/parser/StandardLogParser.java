package com.logviewpro.parser;

import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.util.DateTimeUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandardLogParser implements LogParser {

    // Format 1: 2026-11-17 01:15:23.456 [main] INFO  com.example.Service - Message
    private static final Pattern PATTERN_1 = Pattern.compile(
            "^(\\[?(\\d{4}[-/.]\\d{2}[-/.]\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}[.,]?\\d{0,9}(?:[+-]\\d{2}:?\\d{2}|Z)?)\\]?)\\s+(?:\\[([^\\]]+)\\]\\s+)?([A-Z]{3,8})\\s+(?:\\[([^\\]]+)\\]\\s+)?([\\w.$]+)(?:\\s*-\\s*|\\s*:\\s*|\\s+)(.*)$"
    );

    // Format 2: 2026-11-17 01:15:23.456 INFO  [main] com.example.Service - Message
    private static final Pattern PATTERN_2 = Pattern.compile(
            "^(\\[?(\\d{4}[-/.]\\d{2}[-/.]\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}[.,]?\\d{0,9}(?:[+-]\\d{2}:?\\d{2}|Z)?)\\]?)\\s+([A-Z]{3,8})\\s+(?:\\[([^\\]]+)\\]\\s+)?([\\w.$]+)(?:\\s*-\\s*|\\s*:\\s*|\\s+)(.*)$"
    );

    // Format 3: [INFO ] 2026-11-17 01:15:23 [main] com.example.Service - Message
    private static final Pattern PATTERN_3 = Pattern.compile(
            "^\\[([A-Z]{3,8})\\s*\\]\\s+(\\d{4}[-/.]\\d{2}[-/.]\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}[.,]?\\d{0,9})\\s+(?:\\[([^\\]]+)\\]\\s+)?([\\w.$]+)(?:\\s*-\\s*|\\s*:\\s*|\\s+)(.*)$"
    );

    // General fallback: Starts with timestamp followed by log level
    private static final Pattern GENERAL_TIMESTAMP_LEVEL = Pattern.compile(
            "^(\\[?(\\d{4}[-/.]\\d{2}[-/.]\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}[.,]?\\d{0,9}(?:[+-]\\d{2}:?\\d{2}|Z)?)\\]?)\\s+.*?\\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)\\b(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean isNewEntry(String line) {
        if (line == null || line.isBlank()) return false;
        char c0 = line.charAt(0);
        // Continuation lines usually start with whitespace, tab, "at ", "Caused by:", or "..."
        if (Character.isWhitespace(c0) || line.startsWith("at ") || line.startsWith("Caused by:") || line.startsWith("...")) {
            return false;
        }

        return PATTERN_1.matcher(line).matches() ||
               PATTERN_2.matcher(line).matches() ||
               PATTERN_3.matcher(line).matches() ||
               GENERAL_TIMESTAMP_LEVEL.matcher(line).matches();
    }

    @Override
    public LogEntry parseHeader(String line, long lineNumber, String filePath) {
        if (line == null) return null;

        Matcher m1 = PATTERN_1.matcher(line);
        if (m1.matches()) {
            String rawTs = m1.group(2);
            String thread = m1.group(3) != null ? m1.group(3) : (m1.group(5) != null ? m1.group(5) : "");
            String level = m1.group(4);
            String logger = m1.group(6);
            String message = m1.group(7);
            return createEntry(lineNumber, filePath, rawTs, level, thread, logger, message, line);
        }

        Matcher m2 = PATTERN_2.matcher(line);
        if (m2.matches()) {
            String rawTs = m2.group(2);
            String level = m2.group(3);
            String thread = m2.group(4) != null ? m2.group(4) : "";
            String logger = m2.group(5);
            String message = m2.group(6);
            return createEntry(lineNumber, filePath, rawTs, level, thread, logger, message, line);
        }

        Matcher m3 = PATTERN_3.matcher(line);
        if (m3.matches()) {
            String level = m3.group(1);
            String rawTs = m3.group(2);
            String thread = m3.group(3) != null ? m3.group(3) : "";
            String logger = m3.group(4);
            String message = m3.group(5);
            return createEntry(lineNumber, filePath, rawTs, level, thread, logger, message, line);
        }

        Matcher mg = GENERAL_TIMESTAMP_LEVEL.matcher(line);
        if (mg.matches()) {
            String rawTs = mg.group(2);
            String level = mg.group(3);
            String message = mg.group(4) != null ? mg.group(4).trim() : "";
            return createEntry(lineNumber, filePath, rawTs, level, "", "", message, line);
        }

        // Raw fallback
        LogEntry fallback = new LogEntry();
        fallback.setLineNumber(lineNumber);
        fallback.setFilePath(filePath);
        fallback.setMessage(line);
        fallback.setRawText(line);
        fallback.setLevel(LogLevel.UNKNOWN);
        return fallback;
    }

    private LogEntry createEntry(long lineNumber, String filePath, String rawTs,
                                 String level, String thread, String logger,
                                 String message, String rawText) {
        LogEntry entry = new LogEntry();
        entry.setLineNumber(lineNumber);
        entry.setFilePath(filePath);
        entry.setRawTimestamp(rawTs);
        entry.setTimestamp(DateTimeUtil.parseTimestamp(rawTs));
        entry.setLevel(LogLevel.fromString(level));
        entry.setThread(thread != null ? thread.trim() : "");
        entry.setLogger(logger != null ? logger.trim() : "");
        entry.setMessage(message != null ? message.trim() : "");
        entry.setRawText(rawText);
        return entry;
    }

    @Override
    public void appendContinuation(LogEntry entry, String line) {
        if (entry != null && line != null) {
            String currentStack = entry.getStackTrace();
            if (currentStack == null || currentStack.isEmpty()) {
                entry.setStackTrace(line);
            } else {
                entry.setStackTrace(currentStack + "\n" + line);
            }
            entry.setRawText(entry.getRawText() + "\n" + line);
        }
    }
}
