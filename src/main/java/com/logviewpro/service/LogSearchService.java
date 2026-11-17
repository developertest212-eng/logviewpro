package com.logviewpro.service;

import com.logviewpro.index.LargeFileIndex;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.model.SearchQuery;
import com.logviewpro.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogSearchService {
    private static final Logger logger = LoggerFactory.getLogger(LogSearchService.class);

    private final ExecutorService searchExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "LogSearchWorker");
        t.setDaemon(true);
        return t;
    });

    private Future<?> currentSearchTask;
    private final AtomicBoolean searchCancelled = new AtomicBoolean(false);

    public void cancelCurrentSearch() {
        searchCancelled.set(true);
        if (currentSearchTask != null && !currentSearchTask.isDone()) {
            currentSearchTask.cancel(true);
        }
    }

    public Future<?> executeSearchAsync(List<LargeFileIndex> indices,
                                        SearchQuery query,
                                        Consumer<SearchResult> onMatchFound,
                                        Consumer<Double> onProgress,
                                        Runnable onCompleted) {
        cancelCurrentSearch();
        searchCancelled.set(false);

        currentSearchTask = searchExecutor.submit(() -> {
            try {
                // Compile regex / pattern if needed
                Pattern searchPattern = null;
                String queryText = query.getText();
                if (queryText != null && !queryText.isEmpty()) {
                    int flags = query.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
                    if (query.isRegex()) {
                        searchPattern = Pattern.compile(queryText, flags);
                    } else if (query.isWholeWord()) {
                        searchPattern = Pattern.compile("\\b" + Pattern.quote(queryText) + "\\b", flags);
                    } else {
                        searchPattern = Pattern.compile(Pattern.quote(queryText), flags);
                    }
                }

                int totalIndices = indices.size();
                for (int idx = 0; idx < totalIndices; idx++) {
                    if (searchCancelled.get() || Thread.currentThread().isInterrupted()) break;

                    LargeFileIndex index = indices.get(idx);
                    if (index == null) continue;

                    int totalEntries = index.size();
                    int start = (int) Math.max(0, query.getStartLine() - 1);
                    int end = query.getEndLine() > 0 ? (int) Math.min(query.getEndLine(), totalEntries) : totalEntries;

                    for (int i = start; i < end; i++) {
                        if (searchCancelled.get() || Thread.currentThread().isInterrupted()) break;

                        LogEntry entry = index.getEntry(i);
                        if (entry != null && matchesQuery(entry, query, searchPattern)) {
                            SearchResult result = buildSearchResult(entry, searchPattern, queryText);
                            if (result != null && onMatchFound != null) {
                                onMatchFound.accept(result);
                            }
                        }

                        if (onProgress != null && i % 500 == 0 && totalEntries > 0) {
                            double prog = ((double) idx / totalIndices) + (((double) i / totalEntries) / totalIndices);
                            onProgress.accept(prog);
                        }
                    }
                }

                if (onProgress != null && !searchCancelled.get()) {
                    onProgress.accept(1.0);
                }
                if (onCompleted != null && !searchCancelled.get()) {
                    onCompleted.run();
                }
            } catch (Exception e) {
                logger.error("Search execution encountered error: {}", e.getMessage(), e);
            }
        });

        return currentSearchTask;
    }

    private boolean matchesQuery(LogEntry entry, SearchQuery query, Pattern searchPattern) {
        // Level filter
        if (query.getLevelFilter() != null && entry.getLevel() != query.getLevelFilter()) {
            return false;
        }

        // Class name filter
        if (!query.getClassNameFilter().isEmpty()) {
            if (entry.getLogger() == null || !entry.getLogger().toLowerCase().contains(query.getClassNameFilter().toLowerCase())) {
                return false;
            }
        }

        // Thread filter
        if (!query.getThreadFilter().isEmpty()) {
            if (entry.getThread() == null || !entry.getThread().toLowerCase().contains(query.getThreadFilter().toLowerCase())) {
                return false;
            }
        }

        // Exception filter
        if (!query.getExceptionFilter().isEmpty()) {
            if (entry.getStackTrace() == null || !entry.getStackTrace().toLowerCase().contains(query.getExceptionFilter().toLowerCase())) {
                return false;
            }
        }

        // Timestamp range filter
        if (query.getFromTimestamp() != null && entry.getTimestamp() != null) {
            if (entry.getTimestamp().isBefore(query.getFromTimestamp())) {
                return false;
            }
        }
        if (query.getToTimestamp() != null && entry.getTimestamp() != null) {
            if (entry.getTimestamp().isAfter(query.getToTimestamp())) {
                return false;
            }
        }

        // Text search
        if (searchPattern != null) {
            String fullSearchable = entry.getMessage() + "\n" + entry.getStackTrace();
            return searchPattern.matcher(fullSearchable).find();
        }

        return true;
    }

    private SearchResult buildSearchResult(LogEntry entry, Pattern searchPattern, String queryText) {
        String msg = entry.getMessage();
        int matchStart = -1;
        int matchLength = 0;

        if (searchPattern != null) {
            Matcher m = searchPattern.matcher(msg);
            if (m.find()) {
                matchStart = m.start();
                matchLength = m.end() - m.start();
            } else if (entry.hasStackTrace()) {
                Matcher mStack = searchPattern.matcher(entry.getStackTrace());
                if (mStack.find()) {
                    matchStart = mStack.start();
                    matchLength = mStack.end() - mStack.start();
                    msg = entry.getStackTrace();
                }
            }
        }

        String snippet = msg;
        if (snippet != null && snippet.length() > 200) {
            int start = Math.max(0, matchStart - 30);
            int end = Math.min(snippet.length(), matchStart + 170);
            snippet = (start > 0 ? "..." : "") + snippet.substring(start, end) + (end < snippet.length() ? "..." : "");
        }

        return new SearchResult(entry.getFilePath(), entry.getLineNumber(), entry, matchStart, matchLength, snippet != null ? snippet.trim() : "");
    }
}
