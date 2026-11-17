package com.logviewpro.index;

import com.logviewpro.model.LogEntry;
import com.logviewpro.parser.CompositeLogParser;
import com.logviewpro.parser.LogParser;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class FileChunkReader {

    private final LogParser parser = new CompositeLogParser();

    public List<LogEntry> readEntriesRange(LargeFileIndex index, int startIndex, int count) {
        List<LogEntry> results = new ArrayList<>();
        if (index == null || startIndex < 0 || count <= 0) {
            return results;
        }

        int total = index.size();
        int endIndex = Math.min(startIndex + count, total);

        for (int i = startIndex; i < endIndex; i++) {
            LogEntry entry = index.getEntry(i);
            if (entry != null) {
                results.add(entry);
            }
        }
        return results;
    }

    public static String readRawSnippet(File file, long offset, int length, Charset charset) {
        if (file == null || !file.exists() || offset < 0 || length <= 0) {
            return "";
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            byte[] bytes = new byte[Math.min(length, 1024 * 1024)];
            int read = raf.read(bytes);
            if (read > 0) {
                return new String(bytes, 0, read, charset);
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
