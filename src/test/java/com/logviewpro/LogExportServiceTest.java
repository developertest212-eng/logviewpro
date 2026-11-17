package com.logviewpro;

import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.service.LogExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogExportServiceTest {

    @Test
    public void testExportAllFormats(@TempDir Path tempDir) throws Exception {
        LogExportService service = new LogExportService();

        LogEntry e1 = new LogEntry(1, 1, LocalDateTime.now(), "2026-11-17 01:00:00.123",
                LogLevel.INFO, "main", "com.example.App", "App started successfully", "", "App started successfully", "app.log");
        LogEntry e2 = new LogEntry(2, 2, LocalDateTime.now(), "2026-11-17 01:00:01.456",
                LogLevel.ERROR, "http-1", "com.example.Order", "Order failed",
                "java.lang.NullPointerException: null\n\tat com.example.Order.pay(Order.java:12)", "Order failed", "app.log");

        List<LogEntry> entries = List.of(e1, e2);

        // 1. TXT
        File txtFile = tempDir.resolve("export.txt").toFile();
        service.exportToTxt(txtFile, entries, "Test Header");
        assertTrue(txtFile.exists() && txtFile.length() > 0);

        // 2. CSV
        File csvFile = tempDir.resolve("export.csv").toFile();
        service.exportToCsv(csvFile, entries, "Test Header");
        assertTrue(csvFile.exists() && csvFile.length() > 0);

        // 3. JSON
        File jsonFile = tempDir.resolve("export.json").toFile();
        service.exportToJson(jsonFile, entries);
        assertTrue(jsonFile.exists() && jsonFile.length() > 0);

        // 4. HTML
        File htmlFile = tempDir.resolve("export.html").toFile();
        service.exportToHtml(htmlFile, entries, "Test Title", "Metadata Info");
        assertTrue(htmlFile.exists() && htmlFile.length() > 0);

        // 5. PDF
        File pdfFile = tempDir.resolve("export.pdf").toFile();
        service.exportToPdf(pdfFile, entries, "Test Title", "Subtitle");
        assertTrue(pdfFile.exists() && pdfFile.length() > 0);
    }
}
