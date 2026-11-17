package com.logviewpro.parser;

import com.logviewpro.model.LogEntry;

import java.util.List;

public class CompositeLogParser implements LogParser {

    private final JsonLogParser jsonParser = new JsonLogParser();
    private final SpringBootLogParser springBootParser = new SpringBootLogParser();
    private final StandardLogParser standardParser = new StandardLogParser();

    private final List<LogParser> parsers = List.of(jsonParser, springBootParser, standardParser);

    @Override
    public boolean isNewEntry(String line) {
        if (line == null || line.isBlank()) return false;
        String trimmed = line.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return true;
        }
        if (springBootParser.isNewEntry(line)) {
            return true;
        }
        return standardParser.isNewEntry(line);
    }

    @Override
    public LogEntry parseHeader(String line, long lineNumber, String filePath) {
        if (line == null) return null;
        String trimmed = line.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            LogEntry entry = jsonParser.parseHeader(line, lineNumber, filePath);
            if (entry != null) return entry;
        }
        if (springBootParser.isNewEntry(line)) {
            LogEntry entry = springBootParser.parseHeader(line, lineNumber, filePath);
            if (entry != null) return entry;
        }
        return standardParser.parseHeader(line, lineNumber, filePath);
    }

    @Override
    public void appendContinuation(LogEntry entry, String line) {
        standardParser.appendContinuation(entry, line);
    }
}
