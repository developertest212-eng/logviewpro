package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.index.FileChunkReader;
import com.logviewpro.index.LargeFileIndex;
import com.logviewpro.model.ComparisonResult;
import com.logviewpro.model.DiffEntry;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogFileMeta;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class LogComparisonController {

    @FXML private ComboBox<String> comboFileA;
    @FXML private ComboBox<String> comboFileB;
    @FXML private CheckBox chkIgnoreTimestamps;
    @FXML private Button btnCompare;
    @FXML private Button btnExportDiff;

    @FXML private Label lblStatsA;
    @FXML private Label lblStatsB;
    @FXML private Label lblDiffSummary;

    @FXML private TableView<DiffEntry> tableDiff;
    @FXML private TableColumn<DiffEntry, String> colDiffType;
    @FXML private TableColumn<DiffEntry, String> colDiffDesc;
    @FXML private TableColumn<DiffEntry, String> colMessageA;
    @FXML private TableColumn<DiffEntry, String> colMessageB;

    private final AppContext context = AppContext.getInstance();
    private final FileChunkReader chunkReader = new FileChunkReader();
    private final ObservableList<DiffEntry> diffList = FXCollections.observableArrayList();
    private ComparisonResult lastResult;

    @FXML
    public void initialize() {
        colDiffType.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType().name()));
        colDiffType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "ADDED" -> setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold;");
                        case "REMOVED" -> setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                        case "MODIFIED" -> setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: #9CA3AF;");
                    }
                }
            }
        });

        colDiffDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colMessageA.setCellValueFactory(cellData -> {
            LogEntry a = cellData.getValue().getEntryA();
            return new javafx.beans.property.SimpleStringProperty(a != null ? a.getRawText() : "");
        });
        colMessageB.setCellValueFactory(cellData -> {
            LogEntry b = cellData.getValue().getEntryB();
            return new javafx.beans.property.SimpleStringProperty(b != null ? b.getRawText() : "");
        });

        tableDiff.setItems(diffList);

        refreshFileChoices();
    }

    public void refreshFileChoices() {
        comboFileA.getItems().clear();
        comboFileB.getItems().clear();
        for (LogFileMeta meta : context.getFileService().getOpenFiles()) {
            comboFileA.getItems().add(meta.getFilePath());
            comboFileB.getItems().add(meta.getFilePath());
        }
        if (comboFileA.getItems().size() >= 2) {
            comboFileA.getSelectionModel().select(0);
            comboFileB.getSelectionModel().select(1);
        } else if (!comboFileA.getItems().isEmpty()) {
            comboFileA.getSelectionModel().select(0);
            comboFileB.getSelectionModel().select(0);
        }
    }

    @FXML
    private void onCompare() {
        String pathA = comboFileA.getValue();
        String pathB = comboFileB.getValue();
        if (pathA == null || pathB == null) return;

        LargeFileIndex indexA = context.getFileService().getIndex(pathA);
        LargeFileIndex indexB = context.getFileService().getIndex(pathB);
        if (indexA == null || indexB == null) return;

        new Thread(() -> {
            int limitA = Math.min(indexA.size(), 3000);
            int limitB = Math.min(indexB.size(), 3000);
            List<LogEntry> listA = chunkReader.readEntriesRange(indexA, 0, limitA);
            List<LogEntry> listB = chunkReader.readEntriesRange(indexB, 0, limitB);

            ComparisonResult result = context.getComparisonService().compareLogs(
                    listA, listB, pathA, pathB, chkIgnoreTimestamps.isSelected()
            );
            lastResult = result;

            Platform.runLater(() -> {
                diffList.setAll(result.getDiffEntries());
                lblStatsA.setText(String.format("File A: %,d entries | %,d errors | %,d warnings",
                        result.getTotalA(), result.getErrorsA(), result.getWarningsA()));
                lblStatsB.setText(String.format("File B: %,d entries | %,d errors | %,d warnings",
                        result.getTotalB(), result.getErrorsB(), result.getWarningsB()));
                lblDiffSummary.setText(String.format("Identical: %,d | Added (+): %,d | Removed (-): %,d",
                        result.getIdenticalCount(), result.getAddedCount(), result.getRemovedCount()));
                btnExportDiff.setDisable(false);
            });
        }).start();
    }

    @FXML
    private void onExportDiff() {
        if (lastResult == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Comparison Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Report", "*.html"));
        File file = fc.showSaveDialog(tableDiff.getScene().getWindow());
        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("<html><head><title>Log Comparison Report</title><style>body{font-family:sans-serif;background:#111827;color:#F3F4F6;padding:20px;}table{width:100%;border-collapse:collapse;}th,td{padding:8px;border:1px solid #374151;}th{background:#1F2937;}.ADDED{color:#22C55E;}.REMOVED{color:#EF4444;}</style></head><body>");
                fw.write("<h1>Log Comparison Report</h1>");
                fw.write("<p>File A: " + lastResult.getFileAPath() + "<br>File B: " + lastResult.getFileBPath() + "</p>");
                fw.write("<p>Identical: " + lastResult.getIdenticalCount() + " | Added: " + lastResult.getAddedCount() + " | Removed: " + lastResult.getRemovedCount() + "</p>");
                fw.write("<table><thead><tr><th>Type</th><th>File A</th><th>File B</th></tr></thead><tbody>");
                for (DiffEntry de : lastResult.getDiffEntries()) {
                    fw.write("<tr class=\"" + de.getType() + "\"><td>" + de.getType() + "</td><td>" + (de.getEntryA() != null ? de.getEntryA().getRawText() : "") + "</td><td>" + (de.getEntryB() != null ? de.getEntryB().getRawText() : "") + "</td></tr>");
                }
                fw.write("</tbody></table></body></html>");
            } catch (IOException ignored) {}
        }
    }
}
