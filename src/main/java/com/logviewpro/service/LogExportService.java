package com.logviewpro.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class LogExportService {
    private static final Logger logger = LoggerFactory.getLogger(LogExportService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public void exportToTxt(File destFile, List<LogEntry> entries, String metadataHeader) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8))) {
            if (metadataHeader != null && !metadataHeader.isBlank()) {
                writer.write("# ========================================================\n");
                writer.write("# LogView Pro Export\n");
                writer.write(metadataHeader);
                writer.write("# ========================================================\n\n");
            }
            for (LogEntry entry : entries) {
                writer.write(entry.getRawText());
                writer.newLine();
            }
        }
    }

    public void exportToCsv(File destFile, List<LogEntry> entries, String metadataHeader) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8))) {
            // CSV Header
            writer.write("LineNumber,Timestamp,Level,Thread,Logger,Message\n");
            for (LogEntry entry : entries) {
                writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        entry.getLineNumber(),
                        escapeCsv(entry.getRawTimestamp()),
                        escapeCsv(entry.getLevel().getDisplayName()),
                        escapeCsv(entry.getThread()),
                        escapeCsv(entry.getLogger()),
                        escapeCsv(entry.getMessage() + (entry.hasStackTrace() ? "\n" + entry.getStackTrace() : ""))
                ));
            }
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    public void exportToJson(File destFile, List<LogEntry> entries) throws IOException {
        MAPPER.writeValue(destFile, entries);
    }

    public void exportToHtml(File destFile, List<LogEntry> entries, String reportTitle, String metadataInfo) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8))) {
            writer.write("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>LogView Pro - Report</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                        h1 { color: #818CF8; margin-bottom: 8px; }
                        .meta { background: #1E293B; padding: 16px; border-radius: 8px; margin-bottom: 24px; border: 1px solid #334155; }
                        table { width: 100%; border-collapse: collapse; font-family: 'Consolas', 'Courier New', monospace; font-size: 13px; }
                        th { background: #1E293B; padding: 10px 12px; text-align: left; color: #94A3B8; border-bottom: 2px solid #334155; }
                        td { padding: 8px 12px; border-bottom: 1px solid #1E293B; vertical-align: top; }
                        tr:hover { background: #1E293B; }
                        .badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; }
                        .badge-ERROR, .badge-FATAL { background: #7F1D1D; color: #FCA5A5; }
                        .badge-WARN { background: #78350F; color: #FCD34D; }
                        .badge-INFO { background: #1E3A8A; color: #93C5FD; }
                        .badge-DEBUG { background: #064E3B; color: #6EE7B7; }
                        .badge-TRACE { background: #374151; color: #D1D5DB; }
                        .stacktrace { color: #F87171; white-space: pre-wrap; font-size: 11px; margin-top: 4px; }
                    </style>
                </head>
                <body>
            """);

            writer.write("<h1>" + reportTitle + "</h1>\n");
            writer.write("<div class=\"meta\">\n");
            writer.write("<p><strong>Generated At:</strong> " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>\n");
            if (metadataInfo != null) {
                writer.write("<p>" + metadataInfo.replace("\n", "<br>") + "</p>\n");
            }
            writer.write("<p><strong>Total Log Entries:</strong> " + entries.size() + "</p>\n");
            writer.write("</div>\n");

            writer.write("""
                <table>
                    <thead>
                        <tr>
                            <th style="width: 60px;">Line</th>
                            <th style="width: 170px;">Timestamp</th>
                            <th style="width: 80px;">Level</th>
                            <th style="width: 130px;">Thread</th>
                            <th style="width: 200px;">Logger</th>
                            <th>Message</th>
                        </tr>
                    </thead>
                    <tbody>
            """);

            for (LogEntry e : entries) {
                String badgeClass = "badge badge-" + e.getLevel().getDisplayName();
                writer.write("<tr>");
                writer.write("<td>" + e.getLineNumber() + "</td>");
                writer.write("<td>" + (e.getRawTimestamp() != null ? e.getRawTimestamp() : "") + "</td>");
                writer.write("<td><span class=\"" + badgeClass + "\">" + e.getLevel().getDisplayName() + "</span></td>");
                writer.write("<td>" + escapeHtml(e.getThread()) + "</td>");
                writer.write("<td>" + escapeHtml(e.getLogger()) + "</td>");
                writer.write("<td>" + escapeHtml(e.getMessage()));
                if (e.hasStackTrace()) {
                    writer.write("<div class=\"stacktrace\">" + escapeHtml(e.getStackTrace()) + "</div>");
                }
                writer.write("</td></tr>\n");
            }

            writer.write("""
                    </tbody>
                </table>
                </body>
                </html>
            """);
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public void exportToPdf(File destFile, List<LogEntry> entries, String title, String subtitle) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        PdfWriter.getInstance(document, new FileOutputStream(destFile));
        document.open();

        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        com.lowagie.text.Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        com.lowagie.text.Font cellFont = FontFactory.getFont(FontFactory.COURIER, 8, Color.BLACK);
        com.lowagie.text.Font stackFont = FontFactory.getFont(FontFactory.COURIER, 7, Color.RED.darker());

        document.add(new Paragraph(title, titleFont));
        document.add(new Paragraph((subtitle != null ? subtitle + " | " : "") +
                "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                " | Total Entries: " + entries.size(), subFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.0f, 2.5f, 1.2f, 2.0f, 3.0f, 6.0f});

        String[] headers = {"Line", "Timestamp", "Level", "Thread", "Logger", "Message"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new Color(30, 41, 59));
            cell.setPadding(5);
            table.addCell(cell);
        }

        // Cap PDF lines to first 2,000 entries to avoid multi-hundred page PDF timeouts
        int limit = Math.min(entries.size(), 2000);
        for (int i = 0; i < limit; i++) {
            LogEntry e = entries.get(i);
            table.addCell(new Phrase(String.valueOf(e.getLineNumber()), cellFont));
            table.addCell(new Phrase(e.getRawTimestamp() != null ? e.getRawTimestamp() : "", cellFont));

            PdfPCell levelCell = new PdfPCell(new Phrase(e.getLevel().getDisplayName(), cellFont));
            if (e.getLevel() == LogLevel.ERROR || e.getLevel() == LogLevel.FATAL) {
                levelCell.setBackgroundColor(new Color(254, 226, 226));
            } else if (e.getLevel() == LogLevel.WARN) {
                levelCell.setBackgroundColor(new Color(254, 243, 199));
            }
            table.addCell(levelCell);

            table.addCell(new Phrase(e.getThread(), cellFont));
            table.addCell(new Phrase(e.getLogger(), cellFont));

            Phrase msgPhrase = new Phrase(e.getMessage() + "\n", cellFont);
            if (e.hasStackTrace()) {
                String shortStack = e.getStackTrace();
                if (shortStack.length() > 300) shortStack = shortStack.substring(0, 300) + "...";
                msgPhrase.add(new Chunk(shortStack, stackFont));
            }
            table.addCell(msgPhrase);
        }

        document.add(table);
        document.close();
    }
}
