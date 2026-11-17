package com.logviewpro.parser;

import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.util.DateTimeUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpringBootLogParser implements LogParser {

    private static final Pattern SPRING_BOOT_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}[.,]\\d{3}(?:[+-]\\d{2}:\\d{2}|Z)?)\\s+([A-Z]+)\\s+(\\d+)?\\s*---\\s+\\[\\s*([^\\]]+)\\s*\\]\\s+([^\\s:]+)\\s*:\\s*(.*)$"
    );

    @Override
    public boolean isNewEntry(String line) {
        if (line == null || line.isEmpty()) return false;
        char c0 = line.charAt(0);
        if (c0 != '2' && c0 != '1') return false;
        return SPRING_BOOT_PATTERN.matcher(line).matches();
    }

    @Override
    public LogEntry parseHeader(String line, long lineNumber, String filePath) {
        if (line == null) return null;
        Matcher m = SPRING_BOOT_PATTERN.matcher(line);
        if (m.matches()) {
            String rawTs = m.group(1);
            String rawLevel = m.group(2);
            String thread = m.group(4);
            String logger = m.group(5);
            String message = m.group(6);

            LogEntry entry = new LogEntry();
            entry.setLineNumber(lineNumber);
            entry.setFilePath(filePath);
            entry.setRawTimestamp(rawTs);
            entry.setTimestamp(DateTimeUtil.parseTimestamp(rawTs));
            entry.setLevel(LogLevel.fromString(rawLevel));
            entry.setThread(thread != null ? thread.trim() : "");
            entry.setLogger(logger != null ? logger.trim() : "");
            entry.setMessage(message != null ? message : "");
            entry.setRawText(line);
            return entry;
        }
        return null;
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
