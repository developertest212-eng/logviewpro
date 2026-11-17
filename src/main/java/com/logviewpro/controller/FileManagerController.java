package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.model.LogFileMeta;
import com.logviewpro.util.SystemUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class FileManagerController {

    @FXML private VBox dropZone;
    @FXML private TableView<LogFileMeta> tableFiles;
    @FXML private TableColumn<LogFileMeta, Boolean> colPinned;
    @FXML private TableColumn<LogFileMeta, String> colFileName;
    @FXML private TableColumn<LogFileMeta, String> colSize;
    @FXML private TableColumn<LogFileMeta, Long> colLines;
    @FXML private TableColumn<LogFileMeta, String> colEncoding;
    @FXML private TableColumn<LogFileMeta, Long> colErrors;
    @FXML private TableColumn<LogFileMeta, String> colPath;

    @FXML private Label lblActiveFileMeta;
    @FXML private ProgressBar progressLoad;

    private final AppContext context = AppContext.getInstance();
    private final ObservableList<LogFileMeta> fileList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colPinned.setCellValueFactory(cellData -> cellData.getValue().pinnedProperty());
        colPinned.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().add("button-secondary");
                btn.setStyle("-fx-padding: 2 6; -fx-font-size: 11px;");
                btn.setOnAction(e -> {
                    LogFileMeta meta = getTableView().getItems().get(getIndex());
                    context.getFileService().togglePin(meta.getFilePath());
                    refreshFiles();
                });
            }
            @Override
            protected void updateItem(Boolean pinned, boolean empty) {
                super.updateItem(pinned, empty);
                if (empty || pinned == null) {
                    setGraphic(null);
                } else {
                    btn.setText(pinned ? "★ Pinned" : "☆ Pin");
                    btn.setStyle(pinned ? "-fx-background-color: #312E81; -fx-text-fill: #FDE047;" : "");
                    setGraphic(btn);
                }
            }
        });

        colFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colSize.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFormattedSize()));
        colLines.setCellValueFactory(new PropertyValueFactory<>("lineCount"));
        colEncoding.setCellValueFactory(new PropertyValueFactory<>("encoding"));
        colErrors.setCellValueFactory(new PropertyValueFactory<>("errorCount"));
        colPath.setCellValueFactory(new PropertyValueFactory<>("filePath"));

        tableFiles.setItems(fileList);

        // Double click to open into active viewer
        tableFiles.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                LogFileMeta selected = tableFiles.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    activateFile(selected);
                }
            }
        });

        // Setup Drag & Drop
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                for (File f : db.getFiles()) {
                    loadFile(f);
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        refreshFiles();
    }

    public void refreshFiles() {
        fileList.clear();
        fileList.addAll(context.getFileService().getOpenFiles());

        // Also fetch recent from SQLite if open files list is small
        if (fileList.size() < 10) {
            List<LogFileMeta> recents = context.getDbService().getRecentFiles(15);
            for (LogFileMeta r : recents) {
                if (fileList.stream().noneMatch(f -> f.getFilePath().equals(r.getFilePath()))) {
                    fileList.add(r);
                }
            }
        }
    }

    @FXML
    private void onOpenFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Log File");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Log Files", "*.log", "*.txt", "*.json", "*.jsonl", "*.out", "*.trace"),
                new FileChooser.ExtensionFilter("Log Files (*.log)", "*.log"),
                new FileChooser.ExtensionFilter("JSON Logs (*.json, *.jsonl)", "*.json", "*.jsonl"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );
        File file = fc.showOpenDialog(dropZone.getScene().getWindow());
        if (file != null) {
            loadFile(file);
        }
    }

    @FXML
    private void onOpenMultipleFiles() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Multiple Log Files");
        List<File> files = fc.showOpenMultipleDialog(dropZone.getScene().getWindow());
        if (files != null) {
            for (File f : files) {
                loadFile(f);
            }
        }
    }

    @FXML
    private void onOpenFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Log Folder");
        File dir = dc.showDialog(dropZone.getScene().getWindow());
        if (dir != null) {
            progressLoad.setVisible(true);
            new Thread(() -> {
                context.getFileService().openFolder(dir, d -> javafx.application.Platform.runLater(() -> progressLoad.setProgress(d)));
                javafx.application.Platform.runLater(() -> {
                    progressLoad.setVisible(false);
                    refreshFiles();
                });
            }).start();
        }
    }

    private void loadFile(File file) {
        progressLoad.setVisible(true);
        new Thread(() -> {
            LogFileMeta meta = context.getFileService().openFile(file, d -> javafx.application.Platform.runLater(() -> progressLoad.setProgress(d)));
            javafx.application.Platform.runLater(() -> {
                progressLoad.setVisible(false);
                if (meta != null) {
                    activateFile(meta);
                    refreshFiles();
                }
            });
        }).start();
    }

    private void activateFile(LogFileMeta meta) {
        context.setCurrentFileMeta(meta);
        context.setCurrentIndex(context.getFileService().getIndex(meta.getFilePath()));
        lblActiveFileMeta.setText("Active: " + meta.getFileName() + " (" + meta.getFormattedSize() + ", " + meta.getLineCount() + " lines, " + meta.getEncoding() + ")");
    }

    @FXML
    private void onReloadFile() {
        LogFileMeta selected = tableFiles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            loadFile(new File(selected.getFilePath()));
        }
    }

    @FXML
    private void onCopyFilePath() {
        LogFileMeta selected = tableFiles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            SystemUtil.copyToClipboard(selected.getFilePath());
        }
    }

    @FXML
    private void onCloseSelectedFile() {
        LogFileMeta selected = tableFiles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            context.getFileService().closeFile(selected.getFilePath());
            refreshFiles();
        }
    }
}
