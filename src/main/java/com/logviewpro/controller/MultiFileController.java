package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.index.FileChunkReader;
import com.logviewpro.index.LargeFileIndex;
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
import java.util.*;

public class MultiFileController {

    @FXML private TabPane tabPaneFiles;
    @FXML private Button btnOpenNewTab;
    @FXML private Button btnMergeAllByTimestamp;
    @FXML private Label lblMergeStatus;

    @FXML private TableView<LogEntry> tableMergedView;
    @FXML private TableColumn<LogEntry, String> colMergedFile;
    @FXML private TableColumn<LogEntry, Long> colMergedLine;
    @FXML private TableColumn<LogEntry, String> colMergedTime;
    @FXML private TableColumn<LogEntry, String> colMergedLevel;
    @FXML private TableColumn<LogEntry, String> colMergedMessage;

    private final AppContext context = AppContext.getInstance();
    private final FileChunkReader chunkReader = new FileChunkReader();
    private final ObservableList<LogEntry> mergedList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colMergedFile.setCellValueFactory(cellData -> {
            String path = cellData.getValue().getFilePath();
            String name = path != null ? new File(path).getName() : "";
            return new javafx.beans.property.SimpleStringProperty(name);
        });
        colMergedLine.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        colMergedTime.setCellValueFactory(new PropertyValueFactory<>("rawTimestamp"));
        colMergedLevel.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLevel().getDisplayName()));
        colMergedMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        tableMergedView.setItems(mergedList);

        // Populate tabs from currently open files
        for (LogFileMeta meta : context.getFileService().getOpenFiles()) {
            addFileTab(meta);
        }
    }

    public void addFileTab(LogFileMeta meta) {
        if (meta == null) return;
        Tab tab = new Tab(meta.getFileName());
        tab.setClosable(true);

        TableView<LogEntry> tv = new TableView<>();
        TableColumn<LogEntry, Long> cLine = new TableColumn<>("Line");
        cLine.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        cLine.setPrefWidth(60);

        TableColumn<LogEntry, String> cTime = new TableColumn<>("Timestamp");
        cTime.setCellValueFactory(new PropertyValueFactory<>("rawTimestamp"));
        cTime.setPrefWidth(160);

        TableColumn<LogEntry, String> cLevel = new TableColumn<>("Level");
        cLevel.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLevel().getDisplayName()));
        cLevel.setPrefWidth(80);

        TableColumn<LogEntry, String> cMsg = new TableColumn<>("Message");
        cMsg.setCellValueFactory(new PropertyValueFactory<>("message"));
        cMsg.setPrefWidth(450);

        tv.getColumns().addAll(cLine, cTime, cLevel, cMsg);

        LargeFileIndex index = context.getFileService().getIndex(meta.getFilePath());
        if (index != null) {
            int count = Math.min(index.size(), 2000);
            List<LogEntry> entries = chunkReader.readEntriesRange(index, 0, count);
            tv.setItems(FXCollections.observableArrayList(entries));
        }

        tab.setContent(tv);
        tabPaneFiles.getTabs().add(tab);
    }

    @FXML
    private void onOpenNewTab() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Log File to Open in Tab");
        File file = fc.showOpenDialog(tabPaneFiles.getScene().getWindow());
        if (file != null) {
            LogFileMeta meta = context.getFileService().openFile(file, null);
            if (meta != null) {
                addFileTab(meta);
                tabPaneFiles.getSelectionModel().selectLast();
            }
        }
    }

    @FXML
    private void onMergeAllByTimestamp() {
        Collection<LogFileMeta> openFiles = context.getFileService().getOpenFiles();
        if (openFiles.isEmpty()) {
            lblMergeStatus.setText("No open files to merge");
            return;
        }

        lblMergeStatus.setText("Merging open files chronologically...");
        new Thread(() -> {
            List<LogEntry> allEntries = new ArrayList<>();
            Set<String> uniqueMessages = new HashSet<>();
            int duplicates = 0;

            for (LogFileMeta meta : openFiles) {
                LargeFileIndex index = context.getFileService().getIndex(meta.getFilePath());
                if (index != null) {
                    int count = Math.min(index.size(), 2000);
                    List<LogEntry> sample = chunkReader.readEntriesRange(index, 0, count);
                    for (LogEntry e : sample) {
                        String sig = e.getLevel() + "|" + e.getMessage();
                        if (!uniqueMessages.add(sig)) {
                            duplicates++;
                        }
                        allEntries.add(e);
                    }
                }
            }

            // Sort chronologically by timestamp
            allEntries.sort(Comparator.comparing(LogEntry::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));

            int finalDuplicates = duplicates;
            Platform.runLater(() -> {
                mergedList.setAll(allEntries);
                lblMergeStatus.setText(String.format("Merged %,d entries across %d files. (%d duplicate signatures detected)",
                        allEntries.size(), openFiles.size(), finalDuplicates));
            });
        }).start();
    }
}
