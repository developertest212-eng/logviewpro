package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogFileMeta;
import com.logviewpro.monitor.TailerListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class LiveMonitorController implements TailerListener {

    @FXML private ComboBox<String> comboMonitoredFiles;
    @FXML private Button btnToggleMonitoring;
    @FXML private Button btnPauseResume;
    @FXML private CheckBox chkAutoScroll;
    @FXML private Label lblMonitorStatus;
    @FXML private Label lblBufferCount;

    @FXML private TableView<LogEntry> tableLiveLogs;
    @FXML private TableColumn<LogEntry, Long> colLine;
    @FXML private TableColumn<LogEntry, String> colTime;
    @FXML private TableColumn<LogEntry, String> colLevel;
    @FXML private TableColumn<LogEntry, String> colThread;
    @FXML private TableColumn<LogEntry, String> colLogger;
    @FXML private TableColumn<LogEntry, String> colMessage;

    private final AppContext context = AppContext.getInstance();
    private final ObservableList<LogEntry> liveBuffer = FXCollections.observableArrayList();
    private static final int MAX_LIVE_BUFFER = 2000;

    @FXML
    public void initialize() {
        colLine.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("rawTimestamp"));
        colLevel.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLevel().getDisplayName()));
        colThread.setCellValueFactory(new PropertyValueFactory<>("thread"));
        colLogger.setCellValueFactory(new PropertyValueFactory<>("logger"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        tableLiveLogs.setItems(liveBuffer);

        // Row factory for highlight and level styling
        tableLiveLogs.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isHighlighted()) {
                    setStyle("-fx-background-color: #3B2E1E; -fx-text-fill: #FDE68A;");
                } else {
                    switch (item.getLevel()) {
                        case FATAL, ERROR -> setStyle("-fx-background-color: #2D1515;");
                        case WARN -> setStyle("-fx-background-color: #2D2215;");
                        default -> setStyle("");
                    }
                }
            }
        });

        context.getMonitorService().addListener(this);

        refreshFileList();
    }

    public void refreshFileList() {
        comboMonitoredFiles.getItems().clear();
        for (LogFileMeta meta : context.getFileService().getOpenFiles()) {
            comboMonitoredFiles.getItems().add(meta.getFilePath());
        }
        if (context.getCurrentFileMeta() != null) {
            comboMonitoredFiles.getSelectionModel().select(context.getCurrentFileMeta().getFilePath());
        }
    }

    @FXML
    private void onSelectFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Log File to Monitor");
        File file = fc.showOpenDialog(tableLiveLogs.getScene().getWindow());
        if (file != null) {
            LogFileMeta meta = context.getFileService().openFile(file, null);
            refreshFileList();
            comboMonitoredFiles.getSelectionModel().select(file.getAbsolutePath());
            onToggleMonitoring();
        }
    }

    @FXML
    private void onToggleMonitoring() {
        String path = comboMonitoredFiles.getValue();
        if (path == null || path.isBlank()) return;

        File f = new File(path);
        if (!f.exists()) return;

        if (context.getMonitorService().isMonitoring(path)) {
            context.getMonitorService().stopMonitoring(path);
            btnToggleMonitoring.setText("Start Monitoring");
            btnToggleMonitoring.getStyleClass().remove("button-danger");
            btnToggleMonitoring.getStyleClass().add("button-primary");
            lblMonitorStatus.setText("Status: Stopped");
        } else {
            long startLine = liveBuffer.isEmpty() ? 0 : liveBuffer.get(liveBuffer.size() - 1).getLineNumber();
            context.getMonitorService().startMonitoring(f, startLine);
            btnToggleMonitoring.setText("Stop Monitoring");
            btnToggleMonitoring.getStyleClass().remove("button-primary");
            btnToggleMonitoring.getStyleClass().add("button-danger");
            lblMonitorStatus.setText("Status: Active (Tailing " + f.getName() + ")");
        }
    }

    @FXML
    private void onPauseResume() {
        String path = comboMonitoredFiles.getValue();
        if (path == null) return;

        if (btnPauseResume.getText().contains("Pause")) {
            context.getMonitorService().pauseMonitoring(path);
            btnPauseResume.setText("Resume Tail");
        } else {
            context.getMonitorService().resumeMonitoring(path);
            btnPauseResume.setText("Pause Tail");
        }
    }

    @FXML
    private void onClearBuffer() {
        liveBuffer.clear();
        lblBufferCount.setText("Buffered: 0 lines");
    }

    @Override
    public void onNewEntries(String filePath, List<LogEntry> entries) {
        Platform.runLater(() -> {
            for (LogEntry e : entries) {
                liveBuffer.add(e);
                if (liveBuffer.size() > MAX_LIVE_BUFFER) {
                    liveBuffer.remove(0);
                }
            }
            lblBufferCount.setText("Buffered: " + liveBuffer.size() + " lines");

            if (chkAutoScroll.isSelected() && !liveBuffer.isEmpty()) {
                tableLiveLogs.scrollTo(liveBuffer.size() - 1);
            }
        });
    }

    @Override
    public void onFileRotated(String filePath) {
        Platform.runLater(() -> {
            lblMonitorStatus.setText("Notice: File rotated! Reconnecting tailer...");
        });
    }

    @Override
    public void onFileTruncated(String filePath) {
        Platform.runLater(() -> {
            lblMonitorStatus.setText("Notice: File truncated! Resetting buffer...");
            liveBuffer.clear();
        });
    }

    @Override
    public void onStatusChanged(String filePath, boolean active, String message) {
        Platform.runLater(() -> {
            lblMonitorStatus.setText("Status: " + message);
        });
    }

    @Override
    public void onError(String filePath, Throwable throwable) {
        Platform.runLater(() -> {
            lblMonitorStatus.setText("Error: " + throwable.getMessage());
        });
    }
}
