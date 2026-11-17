package com.logviewpro.service;

import com.logviewpro.model.ComparisonResult;
import com.logviewpro.model.DiffEntry;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;

import java.util.*;

public class ComparisonService {

    public ComparisonResult compareLogs(List<LogEntry> listA, List<LogEntry> listB,
                                       String pathA, String pathB,
                                       boolean ignoreTimestamps) {
        ComparisonResult result = new ComparisonResult();
        result.setFileAPath(pathA);
        result.setFileBPath(pathB);
        result.setIgnoredTimestamps(ignoreTimestamps);
        result.setTotalA(listA.size());
        result.setTotalB(listB.size());

        // Count errors and warnings
        int errA = 0, warnA = 0, errB = 0, warnB = 0;
        for (LogEntry e : listA) {
            if (e.getLevel() == LogLevel.ERROR || e.getLevel() == LogLevel.FATAL) errA++;
            if (e.getLevel() == LogLevel.WARN) warnA++;
        }
        for (LogEntry e : listB) {
            if (e.getLevel() == LogLevel.ERROR || e.getLevel() == LogLevel.FATAL) errB++;
            if (e.getLevel() == LogLevel.WARN) warnB++;
        }
        result.setErrorsA(errA);
        result.setErrorsB(errB);
        result.setWarningsA(warnA);
        result.setWarningsB(warnB);

        // Build comparison map for list A
        Map<String, Queue<LogEntry>> mapA = new HashMap<>();
        for (LogEntry e : listA) {
            String key = generateKey(e, ignoreTimestamps);
            mapA.computeIfAbsent(key, k -> new LinkedList<>()).add(e);
        }

        // Compare against list B
        int identical = 0;
        int added = 0;
        int removed = 0;

        for (LogEntry b : listB) {
            String key = generateKey(b, ignoreTimestamps);
            Queue<LogEntry> queueA = mapA.get(key);
            if (queueA != null && !queueA.isEmpty()) {
                LogEntry a = queueA.poll();
                result.addDiffEntry(new DiffEntry(DiffEntry.DiffType.IDENTICAL, a, b, "Identical log entry"));
                identical++;
            } else {
                result.addDiffEntry(new DiffEntry(DiffEntry.DiffType.ADDED, null, b, "Present only in second log"));
                added++;
            }
        }

        // Remaining elements in mapA are REMOVED
        for (Queue<LogEntry> queue : mapA.values()) {
            while (!queue.isEmpty()) {
                LogEntry a = queue.poll();
                result.addDiffEntry(new DiffEntry(DiffEntry.DiffType.REMOVED, a, null, "Present only in first log"));
                removed++;
            }
        }

        result.setIdenticalCount(identical);
        result.setAddedCount(added);
        result.setRemovedCount(removed);

        return result;
    }

    private String generateKey(LogEntry entry, boolean ignoreTimestamps) {
        if (entry == null) return "";
        if (ignoreTimestamps) {
            return entry.getLevel() + "|" + entry.getLogger() + "|" + entry.getMessage();
        } else {
            return (entry.getRawTimestamp() != null ? entry.getRawTimestamp() : "") + "|" +
                   entry.getLevel() + "|" + entry.getLogger() + "|" + entry.getMessage();
        }
    }
}
