package com.logviewpro.service;

import com.logviewpro.index.LargeFileIndex;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogFileMeta;
import com.logviewpro.model.LogLevel;
import com.logviewpro.util.EncodingDetector;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class LogFileService {
    private static final Logger logger = LoggerFactory.getLogger(LogFileService.class);

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "log", "txt", "json", "jsonl", "out", "trace"
    );

    private final DatabaseService dbService;
    private final Map<String, LargeFileIndex> openFileIndices = new ConcurrentHashMap<>();
    private final Map<String, LogFileMeta> openFileMetas = new ConcurrentHashMap<>();

    public LogFileService(DatabaseService dbService) {
        this.dbService = dbService;
    }

    public boolean isSupportedLogFile(File file) {
        if (file == null || !file.isFile()) return false;
        String ext = FilenameUtils.getExtension(file.getName()).toLowerCase();
        if (SUPPORTED_EXTENSIONS.contains(ext)) return true;
        // Also support rotated files like app.log.1, app.log.2026-11-17, server.out.bak
        String name = file.getName().toLowerCase();
        return name.contains(".log.") || name.contains(".out.") || name.endsWith(".log") || name.endsWith(".out");
    }

    public LogFileMeta openFile(File file, Consumer<Double> progressCallback) {
        if (file == null || !file.exists()) {
            logger.warn("Attempted to open non-existent file: {}", file);
            return null;
        }

        String path = file.getAbsolutePath();
        Charset charset = EncodingDetector.detectEncoding(file);
        LargeFileIndex index = new LargeFileIndex(file, charset, 5000);
        index.buildIndex(progressCallback);

        openFileIndices.put(path, index);

        LogFileMeta meta = new LogFileMeta(file);
        meta.setEncoding(charset.name());
        meta.setLineCount(index.size());

        // Calculate summary level statistics
        computeSummaryStats(index, meta);

        openFileMetas.put(path, meta);
        dbService.recordFileOpened(path, file.getName());

        return meta;
    }

    private void computeSummaryStats(LargeFileIndex index, LogFileMeta meta) {
        int total = index.size();
        long errors = 0, warns = 0, fatal = 0, info = 0, debug = 0, trace = 0;

        // Sample up to 10,000 entries or scan index to compute fast counts
        int sampleLimit = Math.min(total, 5000);
        for (int i = 0; i < sampleLimit; i++) {
            LogEntry entry = index.getEntry(i);
            if (entry != null) {
                if (meta.getFirstTimestamp() == null && entry.getTimestamp() != null) {
                    meta.setFirstTimestamp(entry.getTimestamp());
                }
                if (entry.getTimestamp() != null) {
                    meta.setLastTimestamp(entry.getTimestamp());
                }

                LogLevel level = entry.getLevel();
                if (level == LogLevel.ERROR) errors++;
                else if (level == LogLevel.WARN) warns++;
                else if (level == LogLevel.FATAL) fatal++;
                else if (level == LogLevel.INFO) info++;
                else if (level == LogLevel.DEBUG) debug++;
                else if (level == LogLevel.TRACE) trace++;
            }
        }

        // Extrapolate if total > sampleLimit
        if (total > sampleLimit && sampleLimit > 0) {
            double factor = (double) total / sampleLimit;
            meta.setErrorCount((long) (errors * factor));
            meta.setWarnCount((long) (warns * factor));
            meta.setFatalCount((long) (fatal * factor));
            meta.setInfoCount((long) (info * factor));
            meta.setDebugCount((long) (debug * factor));
            meta.setTraceCount((long) (trace * factor));
        } else {
            meta.setErrorCount(errors);
            meta.setWarnCount(warns);
            meta.setFatalCount(fatal);
            meta.setInfoCount(info);
            meta.setDebugCount(debug);
            meta.setTraceCount(trace);
        }
    }

    public List<LogFileMeta> openFolder(File folder, Consumer<Double> progressCallback) {
        List<LogFileMeta> opened = new ArrayList<>();
        if (folder == null || !folder.isDirectory()) {
            return opened;
        }

        File[] files = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith) ||
                   lower.contains(".log.") || lower.contains(".out.");
        });

        if (files != null) {
            int count = files.length;
            for (int i = 0; i < count; i++) {
                File f = files[i];
                final int idx = i;
                Consumer<Double> subProg = progressCallback != null ?
                        d -> progressCallback.accept((idx + d) / count) : null;
                LogFileMeta meta = openFile(f, subProg);
                if (meta != null) {
                    opened.add(meta);
                }
            }
        }
        return opened;
    }

    public List<File> findRotatedFiles(File baseFile) {
        List<File> rotated = new ArrayList<>();
        if (baseFile == null || !baseFile.exists()) return rotated;

        File parent = baseFile.getParentFile();
        if (parent == null || !parent.isDirectory()) return rotated;

        String baseName = baseFile.getName();
        File[] siblings = parent.listFiles();
        if (siblings != null) {
            for (File sibling : siblings) {
                if (!sibling.equals(baseFile) && sibling.getName().startsWith(baseName)) {
                    rotated.add(sibling);
                }
            }
        }
        rotated.sort(Comparator.comparing(File::getName));
        return rotated;
    }

    public LargeFileIndex getIndex(String filePath) {
        return openFileIndices.get(filePath);
    }

    public LogFileMeta getMeta(String filePath) {
        return openFileMetas.get(filePath);
    }

    public Collection<LogFileMeta> getOpenFiles() {
        return openFileMetas.values();
    }

    public void closeFile(String filePath) {
        LargeFileIndex index = openFileIndices.remove(filePath);
        if (index != null) {
            index.cancelIndexing();
            index.clear();
        }
        openFileMetas.remove(filePath);
    }

    public void togglePin(String filePath) {
        LogFileMeta meta = openFileMetas.get(filePath);
        if (meta != null) {
            boolean newPinned = !meta.isPinned();
            meta.setPinned(newPinned);
            dbService.updatePinnedStatus(filePath, newPinned);
        }
    }
}
