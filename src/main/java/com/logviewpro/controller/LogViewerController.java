package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.index.FileChunkReader;
import com.logviewpro.index.LargeFileIndex;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.util.SystemUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class LogViewerController {

    @FXML private TableView<LogEntry> tableLogs;
    @FXML private TableColumn<LogEntry, Boolean> colBookmark;
    @FXML private TableColumn<LogEntry, Long> colLine;
    @FXML private TableColumn<LogEntry, String> colTimestamp;
    @FXML private TableColumn<LogEntry, String> colLevel;
    @FXML private TableColumn<LogEntry, String> colThread;
    @FXML private TableColumn<LogEntry, String> colLogger;
    @FXML private TableColumn<LogEntry, String> colMessage;

    // Filter bar
    @FXML private ToggleButton btnToggleError;
    @FXML private ToggleButton btnToggleWarn;
    @FXML private ToggleButton btnToggleInfo;
    @FXML private ToggleButton btnToggleDebug;
    @FXML private ToggleButton btnToggleTrace;
    @FXML private TextField txtQuickFilter;
    @FXML private TextField txtJumpLine;

    // Paging
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label lblPageInfo;
    @FXML private ComboBox<Integer> comboPageSize;

    // Detail Pane
    @FXML private VBox detailBox;
    @FXML private Label lblDetailHeader;
    @FXML private TextArea txtDetailMessage;
    @FXML private TextArea txtDetailStackTrace;
    @FXML private Button btnCopyStackTrace;
    @FXML private Button btnCopyEntry;
    @FXML private CheckBox chkWrapLines;

    private final AppContext context = AppContext.getInstance();
    private final FileChunkReader chunkReader = new FileChunkReader();

    private final ObservableList<LogEntry> masterEntries = FXCollections.observableArrayList();
    private FilteredList<LogEntry> filteredEntries;

    private final Set<LogLevel> enabledLevels = EnumSet.allOf(LogLevel.class);

    private int currentPage = 0;
    private int pageSize = 1000;
    private int totalEntriesInFile = 0;

    @FXML
    public void initialize() {
        filteredEntries = new FilteredList<>(masterEntries, p -> true);
        tableLogs.setItems(filteredEntries);

        setupColumns();
        setupDetailPane();
        setupPagingControls();
        setupKeyboardShortcuts();

        context.currentIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentPage = 0;
                loadCurrentPage();
            }
        });

        if (context.getCurrentIndex() != null) {
            loadCurrentPage();
        }
    }

    private void setupColumns() {
        colBookmark.setCellValueFactory(cellData -> cellData.getValue().bookmarkedProperty());
        colBookmark.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    LogEntry item = getTableView().getItems().get(getIndex());
                    item.setBookmarked(!item.isBookmarked());
                    updateIcon(item.isBookmarked());
                });
            }
            private void updateIcon(boolean bm) {
                btn.setText(bm ? "🔖" : "▫");
            }
            @Override
            protected void updateItem(Boolean bm, boolean empty) {
                super.updateItem(bm, empty);
                if (empty || bm == null) {
                    setGraphic(null);
                } else {
                    updateIcon(bm);
                    setGraphic(btn);
                }
            }
        });

        colLine.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("rawTimestamp"));
        colLevel.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLevel().getDisplayName()));
        colLevel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String levelName, boolean empty) {
                super.updateItem(levelName, empty);
                if (empty || levelName == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(levelName);
                    LogLevel level = LogLevel.fromString(levelName);
                    setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold;", level.getHexColor()));
                }
            }
        });

        colThread.setCellValueFactory(new PropertyValueFactory<>("thread"));
        colLogger.setCellValueFactory(new PropertyValueFactory<>("logger"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        // Row factory for highlight and coloring
        tableLogs.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isHighlighted()) {
                    setStyle("-fx-background-color: #3730A3; -fx-text-fill: #E0E7FF;");
                } else {
                    switch (item.getLevel()) {
                        case FATAL -> setStyle("-fx-background-color: #2D0A0A;");
                        case ERROR -> setStyle("-fx-background-color: #231212;");
                        case WARN -> setStyle("-fx-background-color: #231C0E;");
                        default -> setStyle("");
                    }
                }
            }
        });

        // Selection listener for detail area
        tableLogs.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showDetail(newVal);
            }
        });
    }

    private void setupDetailPane() {
        chkWrapLines.selectedProperty().addListener((obs, oldVal, newVal) -> {
            txtDetailMessage.setWrapText(newVal);
            txtDetailStackTrace.setWrapText(newVal);
        });

        btnCopyStackTrace.setOnAction(e -> {
            LogEntry selected = tableLogs.getSelectionModel().getSelectedItem();
            if (selected != null && selected.hasStackTrace()) {
                SystemUtil.copyToClipboard(selected.getStackTrace());
            }
        });

        btnCopyEntry.setOnAction(e -> {
            LogEntry selected = tableLogs.getSelectionModel().getSelectedItem();
            if (selected != null) {
                SystemUtil.copyToClipboard(selected.getRawText());
            }
        });
    }

    private void setupPagingControls() {
        comboPageSize.getItems().addAll(500, 1000, 2500, 5000);
        comboPageSize.setValue(pageSize);
        comboPageSize.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal != pageSize) {
                pageSize = newVal;
                currentPage = 0;
                loadCurrentPage();
            }
        });

        txtQuickFilter.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupKeyboardShortcuts() {
        txtJumpLine.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                onJumpToLine();
            }
        });
    }

    public void loadCurrentPage() {
        LargeFileIndex index = context.getCurrentIndex();
        if (index == null) {
            masterEntries.clear();
            lblPageInfo.setText("No file loaded");
            return;
        }

        totalEntriesInFile = index.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntriesInFile / pageSize));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int startIndex = currentPage * pageSize;
        List<LogEntry> entries = chunkReader.readEntriesRange(index, startIndex, pageSize);

        masterEntries.setAll(entries);
        context.getCurrentVisibleEntries().setAll(entries);

        lblPageInfo.setText(String.format("Page %d of %d (%,d - %,d of %,d entries)",
                currentPage + 1, totalPages,
                startIndex + 1,
                Math.min(startIndex + pageSize, totalEntriesInFile),
                totalEntriesInFile));

        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);

        applyFilters();
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadCurrentPage();
        }
    }

    @FXML
    private void onNextPage() {
        int totalPages = (int) Math.ceil((double) totalEntriesInFile / pageSize);
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadCurrentPage();
        }
    }

    @FXML
    private void onJumpToFirst() {
        currentPage = 0;
        loadCurrentPage();
        if (!tableLogs.getItems().isEmpty()) {
            tableLogs.scrollTo(0);
            tableLogs.getSelectionModel().select(0);
        }
    }

    @FXML
    private void onJumpToLast() {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntriesInFile / pageSize));
        currentPage = totalPages - 1;
        loadCurrentPage();
        if (!tableLogs.getItems().isEmpty()) {
            tableLogs.scrollTo(tableLogs.getItems().size() - 1);
            tableLogs.getSelectionModel().select(tableLogs.getItems().size() - 1);
        }
    }

    @FXML
    private void onJumpToLine() {
        try {
            long targetLine = Long.parseLong(txtJumpLine.getText().trim());
            // Compute page corresponding to targetLine
            int targetIndex = (int) (targetLine - 1);
            currentPage = targetIndex / pageSize;
            loadCurrentPage();

            // Find in current page
            for (int i = 0; i < tableLogs.getItems().size(); i++) {
                if (tableLogs.getItems().get(i).getLineNumber() >= targetLine) {
                    tableLogs.scrollTo(i);
                    tableLogs.getSelectionModel().select(i);
                    break;
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    @FXML
    private void onToggleLevel() {
        enabledLevels.clear();
        if (btnToggleError.isSelected()) {
            enabledLevels.add(LogLevel.ERROR);
            enabledLevels.add(LogLevel.FATAL);
        }
        if (btnToggleWarn.isSelected()) enabledLevels.add(LogLevel.WARN);
        if (btnToggleInfo.isSelected()) enabledLevels.add(LogLevel.INFO);
        if (btnToggleDebug.isSelected()) enabledLevels.add(LogLevel.DEBUG);
        if (btnToggleTrace.isSelected()) enabledLevels.add(LogLevel.TRACE);
        enabledLevels.add(LogLevel.UNKNOWN);

        applyFilters();
    }

    private void applyFilters() {
        String filterText = txtQuickFilter.getText() != null ? txtQuickFilter.getText().toLowerCase().trim() : "";

        filteredEntries.setPredicate(entry -> {
            if (entry == null) return false;
            if (!enabledLevels.contains(entry.getLevel())) return false;

            if (!filterText.isEmpty()) {
                boolean matchesMsg = entry.getMessage().toLowerCase().contains(filterText);
                boolean matchesLogger = entry.getLogger().toLowerCase().contains(filterText);
                boolean matchesThread = entry.getThread().toLowerCase().contains(filterText);
                boolean matchesStack = entry.hasStackTrace() && entry.getStackTrace().toLowerCase().contains(filterText);
                return matchesMsg || matchesLogger || matchesThread || matchesStack;
            }

            return true;
        });
    }

    private void showDetail(LogEntry entry) {
        lblDetailHeader.setText(String.format("Line %d | %s | %s | Thread: %s | Logger: %s",
                entry.getLineNumber(), entry.getRawTimestamp(), entry.getLevel(), entry.getThread(), entry.getLogger()));
        txtDetailMessage.setText(entry.getMessage());

        if (entry.hasStackTrace()) {
            txtDetailStackTrace.setText(entry.getStackTrace());
            txtDetailStackTrace.setVisible(true);
            txtDetailStackTrace.setManaged(true);
            btnCopyStackTrace.setDisable(false);
        } else {
            txtDetailStackTrace.clear();
            txtDetailStackTrace.setVisible(false);
            txtDetailStackTrace.setManaged(false);
            btnCopyStackTrace.setDisable(true);
        }
    }
}
