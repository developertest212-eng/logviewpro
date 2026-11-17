package com.logviewpro.index;

import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.parser.CompositeLogParser;
import com.logviewpro.parser.LogParser;
import com.logviewpro.util.EncodingDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class LargeFileIndex {
    private static final Logger logger = LoggerFactory.getLogger(LargeFileIndex.class);
    private static final int CHUNK_SIZE = 64 * 1024; // 64 KB
    private static final int DEFAULT_CACHE_SIZE = 5000;

    private final File file;
    private final Charset charset;
    private final LogParser parser = new CompositeLogParser();

    // Chunked long array to avoid large contiguous heap allocations
    private static final int BUCKET_SIZE = 65536;
    private final List<long[]> offsetBuckets = new ArrayList<>();
    private final List<int[]> lengthBuckets = new ArrayList<>();
    private int totalEntries = 0;

    // LRU Cache for parsed LogEntry objects
    private final int maxCacheSize;
    private final Map<Integer, LogEntry> entryCache;

    private long lastIndexedFileLength = 0;
    private final AtomicBoolean indexingCancelled = new AtomicBoolean(false);

    public LargeFileIndex(File file) {
        this(file, EncodingDetector.detectEncoding(file), DEFAULT_CACHE_SIZE);
    }

    public LargeFileIndex(File file, Charset charset, int maxCacheSize) {
        this.file = file;
        this.charset = charset != null ? charset : EncodingDetector.detectEncoding(file);
        this.maxCacheSize = maxCacheSize > 0 ? maxCacheSize : DEFAULT_CACHE_SIZE;

        this.entryCache = new LinkedHashMap<>(this.maxCacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, LogEntry> eldest) {
                return size() > LargeFileIndex.this.maxCacheSize;
            }
        };
    }

    public synchronized void cancelIndexing() {
        indexingCancelled.set(true);
    }

    public synchronized boolean isCancelled() {
        return indexingCancelled.get();
    }

    /**
     * Builds or incrementally updates the index of log entries.
     */
    public synchronized void buildIndex(Consumer<Double> progressCallback) {
        indexingCancelled.set(false);
        if (!file.exists()) {
            clear();
            return;
        }

        long currentLength = file.length();
        if (currentLength < lastIndexedFileLength) {
            // File was truncated or rotated
            clear();
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) {
                clear();
                return;
            }

            long currentOffset = lastIndexedFileLength;
            raf.seek(currentOffset);

            byte[] buffer = new byte[CHUNK_SIZE];
            StringBuilder lineBuf = new StringBuilder();
            long lineStartOffset = currentOffset;
            long currentEntryStartOffset = -1;
            int currentEntryIndex = totalEntries;

            int bytesRead;
            long processedBytes = currentOffset;

            while ((bytesRead = raf.read(buffer)) != -1) {
                if (indexingCancelled.get()) {
                    break;
                }

                int chunkPos = 0;
                while (chunkPos < bytesRead) {
                    byte b = buffer[chunkPos];
                    long byteFileOffset = processedBytes + chunkPos;
                    chunkPos++;

                    if (b == '\n' || b == '\r') {
                        // End of line
                        String line = lineBuf.toString();
                        lineBuf.setLength(0);

                        // If \r\n, skip following \n if present
                        if (b == '\r' && chunkPos < bytesRead && buffer[chunkPos] == '\n') {
                            chunkPos++;
                        }

                        if (!line.isEmpty()) {
                            boolean isNew = parser.isNewEntry(line);
                            if (isNew) {
                                if (currentEntryStartOffset != -1) {
                                    int entryLen = (int) (lineStartOffset - currentEntryStartOffset);
                                    addIndexEntry(currentEntryStartOffset, entryLen);
                                }
                                currentEntryStartOffset = lineStartOffset;
                            } else if (currentEntryStartOffset == -1) {
                                // First line encountered
                                currentEntryStartOffset = lineStartOffset;
                            }
                        }
                        lineStartOffset = processedBytes + chunkPos;
                    } else {
                        // Accumulate header candidate characters (stop accumulating if line exceeds 4KB)
                        if (lineBuf.length() < 4096) {
                            lineBuf.append((char) (b & 0xFF));
                        }
                    }
                }

                processedBytes += bytesRead;
                if (progressCallback != null && fileLength > 0) {
                    progressCallback.accept((double) processedBytes / fileLength);
                }
            }

            // Flush the last entry
            if (currentEntryStartOffset != -1 && currentEntryStartOffset < fileLength) {
                int entryLen = (int) (fileLength - currentEntryStartOffset);
                addIndexEntry(currentEntryStartOffset, entryLen);
            }

            lastIndexedFileLength = fileLength;
            if (progressCallback != null) {
                progressCallback.accept(1.0);
            }
        } catch (Exception e) {
            logger.error("Error building index for {}: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    private void addIndexEntry(long offset, int length) {
        int bucketIndex = totalEntries / BUCKET_SIZE;
        int indexInBucket = totalEntries % BUCKET_SIZE;

        if (bucketIndex >= offsetBuckets.size()) {
            offsetBuckets.add(new long[BUCKET_SIZE]);
            lengthBuckets.add(new int[BUCKET_SIZE]);
        }

        offsetBuckets.get(bucketIndex)[indexInBucket] = offset;
        lengthBuckets.get(bucketIndex)[indexInBucket] = length;
        totalEntries++;
    }

    public synchronized int size() {
        return totalEntries;
    }

    public synchronized void clear() {
        offsetBuckets.clear();
        lengthBuckets.clear();
        totalEntries = 0;
        lastIndexedFileLength = 0;
        entryCache.clear();
    }

    public synchronized long getOffset(int index) {
        if (index < 0 || index >= totalEntries) return -1;
        int bucketIndex = index / BUCKET_SIZE;
        int indexInBucket = index % BUCKET_SIZE;
        return offsetBuckets.get(bucketIndex)[indexInBucket];
    }

    public synchronized int getLength(int index) {
        if (index < 0 || index >= totalEntries) return 0;
        int bucketIndex = index / BUCKET_SIZE;
        int indexInBucket = index % BUCKET_SIZE;
        return lengthBuckets.get(bucketIndex)[indexInBucket];
    }

    /**
     * Loads and parses a single entry on demand using the offset index and LRU cache.
     */
    public synchronized LogEntry getEntry(int index) {
        if (index < 0 || index >= totalEntries) return null;

        LogEntry cached = entryCache.get(index);
        if (cached != null) {
            return cached;
        }

        long offset = getOffset(index);
        int length = getLength(index);
        if (offset < 0 || length <= 0) return null;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            byte[] bytes = new byte[Math.min(length, 2 * 1024 * 1024)]; // Cap at 2MB per entry to prevent OOM
            int read = raf.read(bytes);
            if (read > 0) {
                String text = new String(bytes, 0, read, charset);
                String[] lines = text.split("\r?\n");
                if (lines.length > 0) {
                    LogEntry entry = parser.parseHeader(lines[0], index + 1L, file.getAbsolutePath());
                    if (entry == null) {
                        entry = new LogEntry(index + 1, index + 1, null, "", LogLevel.UNKNOWN, "", "", lines[0], "", lines[0], file.getAbsolutePath());
                    }
                    entry.setByteOffset(offset);
                    entry.setByteLength(length);

                    for (int i = 1; i < lines.length; i++) {
                        parser.appendContinuation(entry, lines[i]);
                    }
                    entryCache.put(index, entry);
                    return entry;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed reading log entry at index {}: {}", index, e.getMessage());
        }

        return null;
    }

    public File getFile() {
        return file;
    }

    public Charset getCharset() {
        return charset;
    }

    public long getLastIndexedFileLength() {
        return lastIndexedFileLength;
    }
}
