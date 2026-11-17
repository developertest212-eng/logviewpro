package com.logviewpro.monitor;

import com.logviewpro.model.LogEntry;

import java.util.List;

public interface TailerListener {
    void onNewEntries(String filePath, List<LogEntry> entries);
    void onFileRotated(String filePath);
    void onFileTruncated(String filePath);
    void onStatusChanged(String filePath, boolean active, String message);
    void onError(String filePath, Throwable throwable);
}
