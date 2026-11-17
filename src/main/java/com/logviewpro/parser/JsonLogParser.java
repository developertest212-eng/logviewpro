package com.logviewpro.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.util.DateTimeUtil;

import java.time.LocalDateTime;

public class JsonLogParser implements LogParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean isNewEntry(String line) {
        if (line == null) return false;
        String trimmed = line.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    @Override
    public LogEntry parseHeader(String line, long lineNumber, String filePath) {
        if (line == null) return null;
        try {
            JsonNode node = MAPPER.readTree(line.trim());
            LogEntry entry = new LogEntry();
            entry.setLineNumber(lineNumber);
            entry.setFilePath(filePath);
            entry.setRawText(line);

            // Timestamp fields: timestamp, @timestamp, time, date
            String tsStr = getStringField(node, "timestamp", "@timestamp", "time", "date", "datetime");
            if (tsStr != null) {
                entry.setRawTimestamp(tsStr);
                entry.setTimestamp(DateTimeUtil.parseTimestamp(tsStr));
            }

            // Level fields: level, severity, log.level
            String lvlStr = getStringField(node, "level", "severity", "log_level", "loglevel");
            entry.setLevel(LogLevel.fromString(lvlStr));

            // Thread fields: thread, thread_name, threadName
            entry.setThread(getStringField(node, "thread", "thread_name", "threadName"));

            // Logger fields: logger, logger_name, loggerName, class
            entry.setLogger(getStringField(node, "logger", "logger_name", "loggerName", "class"));

            // Message fields: message, msg, log
            entry.setMessage(getStringField(node, "message", "msg", "log"));

            // Stack trace fields: stack_trace, stacktrace, exception, error
            String stackTrace = getStringField(node, "stack_trace", "stacktrace", "exception", "error");
            if (stackTrace != null) {
                entry.setStackTrace(stackTrace);
            }

            return entry;
        } catch (Exception e) {
            return null;
        }
    }

    private String getStringField(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.hasNonNull(fieldName)) {
                JsonNode child = node.get(fieldName);
                if (child.isTextual()) {
                    return child.asText();
                } else if (child.isObject() || child.isArray()) {
                    return child.toString();
                } else {
                    return child.asText();
                }
            }
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
