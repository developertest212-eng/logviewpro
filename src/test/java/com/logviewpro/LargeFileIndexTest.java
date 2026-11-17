package com.logviewpro;

import com.logviewpro.index.FileChunkReader;
import com.logviewpro.index.LargeFileIndex;
import com.logviewpro.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LargeFileIndexTest {

    @Test
    public void testIndexingSampleLogFile() {
        File sampleFile = new File("sample-logs/app-production.log");
        assertTrue(sampleFile.exists(), "Sample log file should exist");

        LargeFileIndex index = new LargeFileIndex(sampleFile);
        index.buildIndex(null);

        assertTrue(index.size() > 5, "Index should have indexed multiple log entries");

        // Test on-demand retrieval of entry 0
        LogEntry first = index.getEntry(0);
        assertNotNull(first);
        assertEquals(1, first.getLineNumber());
        assertTrue(first.getMessage().contains("LogView Pro Core Service Engine"));

        // Test on-demand retrieval of entry with stack trace
        FileChunkReader reader = new FileChunkReader();
        List<LogEntry> chunk = reader.readEntriesRange(index, 0, index.size());
        boolean foundStackTrace = chunk.stream().anyMatch(LogEntry::hasStackTrace);
        assertTrue(foundStackTrace, "Should have correctly parsed and attached multi-line stack trace");
    }
}
