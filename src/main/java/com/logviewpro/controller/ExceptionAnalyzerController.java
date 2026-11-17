package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.index.FileChunkReader;
import com.logviewpro.model.ExceptionGroup;
import com.logviewpro.model.ExceptionOccurrence;
import com.logviewpro.model.LogEntry;
import com.logviewpro.util.SystemUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class ExceptionAnalyzerController {

    @FXML private TableView<ExceptionGroup> tableExceptions;
    @FXML private TableColumn<ExceptionGroup, String> colExName;
    @FXML private TableColumn<ExceptionGroup, Integer> colExCount;
    @FXML private TableColumn<ExceptionGroup, String> colExAppFrame;
    @FXML private TableColumn<ExceptionGroup, String> colExFirstSeen;
    @FXML private TableColumn<ExceptionGroup, String> colExLastSeen;

    @FXML private Label lblAnalysisTitle;
    @FXML private TextArea txtExplanation;
    @FXML private TextArea txtStackSample;

    @FXML private ListView<String> listOccurrences;
    @FXML private Button btnCompareTraces;
    @FXML private Button btnCopyStackTrace;

    private final AppContext context = AppContext.getInstance();
    private final ObservableList<ExceptionGroup> exceptionGroups = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colExName.setCellValueFactory(new PropertyValueFactory<>("simpleExceptionName"));
        colExCount.setCellValueFactory(new PropertyValueFactory<>("count"));
        colExAppFrame.setCellValueFactory(new PropertyValueFactory<>("applicationFrame"));
        colExFirstSeen.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFirstSeen() != null ? cellData.getValue().getFirstSeen().toString() : ""));
        colExLastSeen.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLastSeen() != null ? cellData.getValue().getLastSeen().toString() : ""));

        tableExceptions.setItems(exceptionGroups);

        tableExceptions.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showExceptionDetail(newVal);
            }
        });

        btnCopyStackTrace.setOnAction(e -> {
            ExceptionGroup selected = tableExceptions.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getFullStackTraceSample() != null) {
                SystemUtil.copyToClipboard(selected.getFullStackTraceSample());
            }
        });

        btnCompareTraces.setOnAction(e -> onCompareTraces());

        runAnalysis();
    }

    public void runAnalysis() {
        new Thread(() -> {
            List<LogEntry> entries = context.getCurrentVisibleEntries();
            if (entries.isEmpty() && context.getCurrentIndex() != null) {
                int count = Math.min(context.getCurrentIndex().size(), 10000);
                entries = new FileChunkReader().readEntriesRange(context.getCurrentIndex(), 0, count);
            }

            List<ExceptionGroup> groups = context.getExceptionService().analyzeExceptions(entries);
            Platform.runLater(() -> {
                exceptionGroups.setAll(groups);
                if (!exceptionGroups.isEmpty()) {
                    tableExceptions.getSelectionModel().select(0);
                } else {
                    lblAnalysisTitle.setText("No exceptions detected in current log sample");
                    txtExplanation.clear();
                    txtStackSample.clear();
                    listOccurrences.getItems().clear();
                }
            });
        }).start();
    }

    private void showExceptionDetail(ExceptionGroup group) {
        lblAnalysisTitle.setText(group.getSimpleExceptionName() + " (" + group.getCount() + " occurrences)");
        txtExplanation.setText(context.getExceptionService().generateFactualExplanation(group));
        txtStackSample.setText(group.getFullStackTraceSample());

        listOccurrences.getItems().clear();
        for (ExceptionOccurrence occ : group.getOccurrences()) {
            listOccurrences.getItems().add(String.format("Line %d | %s | Thread: %s",
                    occ.getLineNumber(), occ.getTimestamp() != null ? occ.getTimestamp() : "", occ.getThread()));
        }
    }

    private void onCompareTraces() {
        if (exceptionGroups.size() >= 2) {
            ExceptionGroup g1 = exceptionGroups.get(0);
            ExceptionGroup g2 = exceptionGroups.get(1);
            String diff = context.getExceptionService().compareStackTraces(g1.getFullStackTraceSample(), g2.getFullStackTraceSample());
            txtStackSample.setText(diff);
        }
    }
}
