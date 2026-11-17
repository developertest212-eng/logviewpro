package com.logviewpro.model;

import java.time.LocalDateTime;

public class SearchQuery {
    private String text = "";
    private boolean caseSensitive = false;
    private boolean wholeWord = false;
    private boolean regex = false;
    private LogLevel levelFilter;
    private String classNameFilter = "";
    private String threadFilter = "";
    private String exceptionFilter = "";
    private LocalDateTime fromTimestamp;
    private LocalDateTime toTimestamp;
    private boolean searchAllFiles = false;
    private long startLine = 1;
    private long endLine = -1; // -1 means until end

    public SearchQuery() {}

    public SearchQuery(String text) {
        this.text = text != null ? text : "";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isWholeWord() {
        return wholeWord;
    }

    public void setWholeWord(boolean wholeWord) {
        this.wholeWord = wholeWord;
    }

    public boolean isRegex() {
        return regex;
    }

    public void setRegex(boolean regex) {
        this.regex = regex;
    }

    public LogLevel getLevelFilter() {
        return levelFilter;
    }

    public void setLevelFilter(LogLevel levelFilter) {
        this.levelFilter = levelFilter;
    }

    public String getClassNameFilter() {
        return classNameFilter;
    }

    public void setClassNameFilter(String classNameFilter) {
        this.classNameFilter = classNameFilter != null ? classNameFilter : "";
    }

    public String getThreadFilter() {
        return threadFilter;
    }

    public void setThreadFilter(String threadFilter) {
        this.threadFilter = threadFilter != null ? threadFilter : "";
    }

    public String getExceptionFilter() {
        return exceptionFilter;
    }

    public void setExceptionFilter(String exceptionFilter) {
        this.exceptionFilter = exceptionFilter != null ? exceptionFilter : "";
    }

    public LocalDateTime getFromTimestamp() {
        return fromTimestamp;
    }

    public void setFromTimestamp(LocalDateTime fromTimestamp) {
        this.fromTimestamp = fromTimestamp;
    }

    public LocalDateTime getToTimestamp() {
        return toTimestamp;
    }

    public void setToTimestamp(LocalDateTime toTimestamp) {
        this.toTimestamp = toTimestamp;
    }

    public boolean isSearchAllFiles() {
        return searchAllFiles;
    }

    public void setSearchAllFiles(boolean searchAllFiles) {
        this.searchAllFiles = searchAllFiles;
    }

    public long getStartLine() {
        return startLine;
    }

    public void setStartLine(long startLine) {
        this.startLine = startLine;
    }

    public long getEndLine() {
        return endLine;
    }

    public void setEndLine(long endLine) {
        this.endLine = endLine;
    }
}
