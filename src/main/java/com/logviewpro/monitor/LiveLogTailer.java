package com.logviewpro.monitor;

import com.logviewpro.model.LogEntry;
import com.logviewpro.parser.CompositeLogParser;
import com.logviewpro.parser.LogParser;
import com.logviewpro.util.EncodingDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveLogTailer {
    private static final Logger logger = LoggerFactory.getLogger(LiveLogTailer.class);

    private final File file;
    private final Charset charset;
    private final TailerListener listener;
    private final LogParser parser = new CompositeLogParser();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private Thread workerThread;
    private WatchService watchService;

    private long lastPosition = 0;
    private long currentLineNumber = 0;
    private LogEntry currentPartialEntry = null;

    public LiveLogTailer(File file, TailerListener listener) {
        this.file = file;
        this.charset = EncodingDetector.detectEncoding(file);
        this.listener = listener;
        if (file.exists()) {
            this.lastPosition = file.length();
        }
    }

    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            paused.set(false);
            workerThread = new Thread(this::runMonitoringLoop, "LogTailer-" + file.getName());
            workerThread.setDaemon(true);
            workerThread.start();
            if (listener != null) {
                listener.onStatusChanged(file.getAbsolutePath(), true, "Monitoring active");
            }
        }
    }

    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (Exception ignored) {}
            }
            if (workerThread != null) {
                workerThread.interrupt();
            }
            if (listener != null) {
                listener.onStatusChanged(file.getAbsolutePath(), false, "Monitoring stopped");
            }
        }
    }

    public void pause() {
        paused.set(true);
        if (listener != null) {
            listener.onStatusChanged(file.getAbsolutePath(), false, "Monitoring paused");
        }
    }

    public void resume() {
        paused.set(false);
        if (listener != null) {
            listener.onStatusChanged(file.getAbsolutePath(), true, "Monitoring resumed");
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isPaused() {
        return paused.get();
    }

    public void setCurrentLineNumber(long lineNumber) {
        this.currentLineNumber = lineNumber;
    }

    private void runMonitoringLoop() {
        Path parentDir = file.getParentFile() != null ? file.getParentFile().toPath() : Paths.get(".");
        try {
            watchService = FileSystems.getDefault().newWatchService();
            parentDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);
        } catch (Exception e) {
            logger.warn("WatchService registration failed for {}, falling back to polling: {}", parentDir, e.getMessage());
        }

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (watchService != null) {
                    WatchKey key = watchService.poll(400, TimeUnit.MILLISECONDS);
                    if (key != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changed = (Path) event.context();
                            if (changed != null && changed.toString().equalsIgnoreCase(file.getName())) {
                                checkForAppendedLines();
                            }
                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            logger.warn("WatchKey no longer valid for directory {}", parentDir);
                        }
                    } else {
                        // Periodic fallback poll
                        checkForAppendedLines();
                    }
                } else {
                    // Pure polling fallback
                    Thread.sleep(500);
                    checkForAppendedLines();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                logger.error("Exception in tailer loop for {}: {}", file.getName(), ex.getMessage());
                if (listener != null) {
                    listener.onError(file.getAbsolutePath(), ex);
                }
            }
        }
    }

    private synchronized void checkForAppendedLines() {
        if (!file.exists()) {
            // File might have been rotated or deleted
            return;
        }

        long length = file.length();
        if (length < lastPosition) {
            // File was truncated or rotated
            logger.info("File {} truncated or rotated (old len: {}, new len: {})", file.getName(), lastPosition, length);
            lastPosition = 0;
            currentLineNumber = 0;
            if (listener != null) {
                listener.onFileTruncated(file.getAbsolutePath());
            }
        }

        if (length > lastPosition) {
            readNewLines(length);
        }
    }

    private void readNewLines(long currentFileLength) {
        if (paused.get()) {
            lastPosition = currentFileLength;
            return;
        }

        List<LogEntry> newEntries = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(lastPosition);

            long bytesToRead = currentFileLength - lastPosition;
            // Cap at 10MB per poll to prevent UI flooding
            int readSize = (int) Math.min(bytesToRead, 10 * 1024 * 1024);
            byte[] bytes = new byte[readSize];
            int read = raf.read(bytes);

            if (read > 0) {
                lastPosition += read;
                String text = new String(bytes, 0, read, charset);
                String[] lines = text.split("\r?\n");

                for (String line : lines) {
                    if (line.isEmpty()) continue;
                    boolean isNew = parser.isNewEntry(line);
                    if (isNew) {
                        if (currentPartialEntry != null) {
                            newEntries.add(currentPartialEntry);
                        }
                        currentLineNumber++;
                        currentPartialEntry = parser.parseHeader(line, currentLineNumber, file.getAbsolutePath());
                        if (currentPartialEntry != null) {
                            currentPartialEntry.setHighlighted(true);
                        }
                    } else {
                        if (currentPartialEntry != null) {
                            parser.appendContinuation(currentPartialEntry, line);
                        } else {
                            currentLineNumber++;
                            currentPartialEntry = new LogEntry(currentLineNumber, currentLineNumber, null, "",
                                    com.logviewpro.model.LogLevel.UNKNOWN, "", "", line, "", line, file.getAbsolutePath());
                            currentPartialEntry.setHighlighted(true);
                        }
                    }
                }

                if (currentPartialEntry != null && newEntries.isEmpty()) {
                    newEntries.add(currentPartialEntry);
                    currentPartialEntry = null;
                } else if (currentPartialEntry != null) {
                    newEntries.add(currentPartialEntry);
                    currentPartialEntry = null;
                }
            }
        } catch (Exception e) {
            logger.error("Error reading new lines from {}: {}", file.getName(), e.getMessage());
            if (listener != null) {
                listener.onError(file.getAbsolutePath(), e);
            }
        }

        if (!newEntries.isEmpty() && listener != null) {
            listener.onNewEntries(file.getAbsolutePath(), newEntries);
        }
    }

    public File getFile() {
        return file;
    }
}
