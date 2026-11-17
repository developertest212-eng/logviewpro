package com.logviewpro.service;

import com.logviewpro.model.LogEntry;
import com.logviewpro.monitor.LiveLogTailer;
import com.logviewpro.monitor.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogMonitorService implements TailerListener {
    private static final Logger logger = LoggerFactory.getLogger(LogMonitorService.class);

    private final Map<String, LiveLogTailer> activeTailers = new ConcurrentHashMap<>();
    private final List<TailerListener> subscribers = new CopyOnWriteArrayList<>();

    public void addListener(TailerListener listener) {
        if (listener != null && !subscribers.contains(listener)) {
            subscribers.add(listener);
        }
    }

    public void removeListener(TailerListener listener) {
        subscribers.remove(listener);
    }

    public synchronized void startMonitoring(File file, long startLineNumber) {
        if (file == null || !file.exists()) return;
        String path = file.getAbsolutePath();

        stopMonitoring(path);

        LiveLogTailer tailer = new LiveLogTailer(file, this);
        tailer.setCurrentLineNumber(startLineNumber);
        activeTailers.put(path, tailer);
        tailer.start();
        logger.info("Started real-time monitoring for {}", path);
    }

    public synchronized void stopMonitoring(String filePath) {
        LiveLogTailer tailer = activeTailers.remove(filePath);
        if (tailer != null) {
            tailer.stop();
            logger.info("Stopped monitoring for {}", filePath);
        }
    }

    public void pauseMonitoring(String filePath) {
        LiveLogTailer tailer = activeTailers.get(filePath);
        if (tailer != null) {
            tailer.pause();
        }
    }

    public void resumeMonitoring(String filePath) {
        LiveLogTailer tailer = activeTailers.get(filePath);
        if (tailer != null) {
            tailer.resume();
        }
    }

    public boolean isMonitoring(String filePath) {
        LiveLogTailer tailer = activeTailers.get(filePath);
        return tailer != null && tailer.isRunning() && !tailer.isPaused();
    }

    public void stopAll() {
        for (LiveLogTailer tailer : activeTailers.values()) {
            tailer.stop();
        }
        activeTailers.clear();
    }

    @Override
    public void onNewEntries(String filePath, List<LogEntry> entries) {
        for (TailerListener sub : subscribers) {
            try {
                sub.onNewEntries(filePath, entries);
            } catch (Exception e) {
                logger.error("Error dispatching onNewEntries: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onFileRotated(String filePath) {
        for (TailerListener sub : subscribers) {
            try {
                sub.onFileRotated(filePath);
            } catch (Exception e) {
                logger.error("Error dispatching onFileRotated: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onFileTruncated(String filePath) {
        for (TailerListener sub : subscribers) {
            try {
                sub.onFileTruncated(filePath);
            } catch (Exception e) {
                logger.error("Error dispatching onFileTruncated: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onStatusChanged(String filePath, boolean active, String message) {
        for (TailerListener sub : subscribers) {
            try {
                sub.onStatusChanged(filePath, active, message);
            } catch (Exception e) {
                logger.error("Error dispatching onStatusChanged: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onError(String filePath, Throwable throwable) {
        for (TailerListener sub : subscribers) {
            try {
                sub.onError(filePath, throwable);
            } catch (Exception e) {
                logger.error("Error dispatching onError: {}", e.getMessage());
            }
        }
    }
}
