package com.logviewpro;

import com.logviewpro.model.ComparisonResult;
import com.logviewpro.model.DiffEntry;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.service.ComparisonService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ComparisonServiceTest {

    @Test
    public void testDiffBetweenLogEntries() {
        ComparisonService service = new ComparisonService();

        LogEntry e1 = new LogEntry(1, 1, null, "2026-11-17 01:00:00", LogLevel.INFO, "main", "App", "Starting", "", "Starting", "a.log");
        LogEntry e2 = new LogEntry(2, 2, null, "2026-11-17 01:00:01", LogLevel.ERROR, "main", "App", "DB error", "", "DB error", "a.log");

        LogEntry e3 = new LogEntry(1, 1, null, "2026-11-17 01:00:00", LogLevel.INFO, "main", "App", "Starting", "", "Starting", "b.log");
        LogEntry e4 = new LogEntry(2, 2, null, "2026-11-17 01:00:02", LogLevel.WARN, "main", "App", "Slow disk", "", "Slow disk", "b.log");

        ComparisonResult result = service.compareLogs(List.of(e1, e2), List.of(e3, e4), "a.log", "b.log", true);

        assertEquals(1, result.getIdenticalCount());
        assertEquals(1, result.getAddedCount());
        assertEquals(1, result.getRemovedCount());
        assertEquals(1, result.getErrorsA());
        assertEquals(0, result.getErrorsB());
    }
}
