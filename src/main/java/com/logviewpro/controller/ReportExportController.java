package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.index.FileChunkReader;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogFileMeta;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class ReportExportController {

    @FXML private ComboBox<String> comboReportType;
    @FXML private ComboBox<String> comboFormat;
    @FXML private CheckBox chkIncludeMetadata;
    @FXML private TextField txtCustomTitle;
    @FXML private Button btnGenerateReport;
    @FXML private Label lblExportStatus;
    @FXML private ProgressBar progressExport;

    private final AppContext context = AppContext.getInstance();
    private final FileChunkReader chunkReader = new FileChunkReader();

    @FXML
    public void initialize() {
        comboReportType.getItems().addAll(
                "Full Log Export",
                "Error & Fatal Summary",
                "Exceptions Report",
                "Filtered Active View"
        );
        comboReportType.setValue("Full Log Export");

        comboFormat.getItems().addAll("HTML", "PDF", "CSV", "JSON", "TXT");
        comboFormat.setValue("HTML");

        btnGenerateReport.setOnAction(e -> onGenerateReport());
    }

    private void onGenerateReport() {
        LogFileMeta meta = context.getCurrentFileMeta();
        if (meta == null) {
            lblExportStatus.setText("No active log file loaded to export.");
            return;
        }

        String format = comboFormat.getValue();
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Log Report");
        fc.setInitialFileName("logview-report." + format.toLowerCase());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(format + " File", "*." + format.toLowerCase()));

        File dest = fc.showSaveDialog(btnGenerateReport.getScene().getWindow());
        if (dest == null) return;

        progressExport.setVisible(true);
        lblExportStatus.setText("Exporting " + format + " report...");

        new Thread(() -> {
            try {
                List<LogEntry> entries;
                if ("Filtered Active View".equals(comboReportType.getValue())) {
                    entries = context.getCurrentVisibleEntries();
                } else {
                    int count = Math.min(context.getCurrentIndex().size(), 10000);
                    entries = chunkReader.readEntriesRange(context.getCurrentIndex(), 0, count);
                }

                if ("Error & Fatal Summary".equals(comboReportType.getValue())) {
                    entries = entries.stream()
                            .filter(e -> e.getLevel() == com.logviewpro.model.LogLevel.ERROR || e.getLevel() == com.logviewpro.model.LogLevel.FATAL)
                            .toList();
                } else if ("Exceptions Report".equals(comboReportType.getValue())) {
                    entries = entries.stream().filter(LogEntry::hasStackTrace).toList();
                }

                String title = txtCustomTitle.getText() != null && !txtCustomTitle.getText().isBlank() ?
                        txtCustomTitle.getText().trim() : ("LogView Pro - " + comboReportType.getValue());
                String metaHeader = "File: " + meta.getFilePath() + "\nSource Size: " + meta.getFormattedSize() + "\nEncoding: " + meta.getEncoding();

                switch (format) {
                    case "TXT" -> context.getExportService().exportToTxt(dest, entries, metaHeader);
                    case "CSV" -> context.getExportService().exportToCsv(dest, entries, metaHeader);
                    case "JSON" -> context.getExportService().exportToJson(dest, entries);
                    case "HTML" -> context.getExportService().exportToHtml(dest, entries, title, metaHeader);
                    case "PDF" -> context.getExportService().exportToPdf(dest, entries, title, metaHeader);
                }

                Platform.runLater(() -> {
                    progressExport.setVisible(false);
                    lblExportStatus.setText("Report exported successfully to: " + dest.getName());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    progressExport.setVisible(false);
                    lblExportStatus.setText("Export failed: " + ex.getMessage());
                });
            }
        }).start();
    }
}
