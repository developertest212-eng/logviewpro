package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.index.LargeFileIndex;
import com.logviewpro.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

public class SearchEngineController {

    @FXML private TextField txtSearchTerm;
    @FXML private CheckBox chkCaseSensitive;
    @FXML private CheckBox chkWholeWord;
    @FXML private CheckBox chkRegex;
    @FXML private CheckBox chkSearchAllFiles;
    @FXML private ComboBox<String> comboLevelFilter;
    @FXML private TextField txtClassFilter;
    @FXML private TextField txtThreadFilter;
    @FXML private TextField txtStartLine;
    @FXML private TextField txtEndLine;

    @FXML private Button btnSearch;
    @FXML private Button btnCancelSearch;
    @FXML private ProgressBar progressSearch;
    @FXML private Label lblMatchStats;

    @FXML private TableView<SearchResult> tableSearchResults;
    @FXML private TableColumn<SearchResult, String> colFile;
    @FXML private TableColumn<SearchResult, Long> colLine;
    @FXML private TableColumn<SearchResult, String> colSnippet;

    @FXML private ListView<String> listSearchHistory;

    private final AppContext context = AppContext.getInstance();
    private final ObservableList<SearchResult> searchResults = FXCollections.observableArrayList();
    private final ObservableList<String> historyList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        comboLevelFilter.getItems().addAll("ANY", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE");
        comboLevelFilter.setValue("ANY");

        colFile.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFilePath()));
        colLine.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        colSnippet.setCellValueFactory(new PropertyValueFactory<>("snippet"));

        tableSearchResults.setItems(searchResults);
        listSearchHistory.setItems(historyList);

        // Click on search history item
        listSearchHistory.setOnMouseClicked(e -> {
            String selected = listSearchHistory.getSelectionModel().getSelectedItem();
            if (selected != null) {
                txtSearchTerm.setText(selected);
                onExecuteSearch();
            }
        });

        // Double click on search result to jump
        tableSearchResults.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                SearchResult r = tableSearchResults.getSelectionModel().getSelectedItem();
                if (r != null && r.getLogEntry() != null) {
                    r.getLogEntry().setHighlighted(true);
                }
            }
        });
    }

    public void setSearchTerm(String term) {
        if (txtSearchTerm != null) {
            txtSearchTerm.setText(term);
            onExecuteSearch();
        }
    }

    @FXML
    private void onExecuteSearch() {
        String term = txtSearchTerm.getText() != null ? txtSearchTerm.getText().trim() : "";
        if (term.isEmpty()) return;

        if (!historyList.contains(term)) {
            historyList.add(0, term);
        }

        SearchQuery query = new SearchQuery(term);
        query.setCaseSensitive(chkCaseSensitive.isSelected());
        query.setWholeWord(chkWholeWord.isSelected());
        query.setRegex(chkRegex.isSelected());
        query.setSearchAllFiles(chkSearchAllFiles.isSelected());

        String lvl = comboLevelFilter.getValue();
        if (lvl != null && !"ANY".equals(lvl)) {
            query.setLevelFilter(LogLevel.fromString(lvl));
        }

        query.setClassNameFilter(txtClassFilter.getText());
        query.setThreadFilter(txtThreadFilter.getText());

        try {
            if (!txtStartLine.getText().isBlank()) query.setStartLine(Long.parseLong(txtStartLine.getText().trim()));
            if (!txtEndLine.getText().isBlank()) query.setEndLine(Long.parseLong(txtEndLine.getText().trim()));
        } catch (NumberFormatException ignored) {}

        List<LargeFileIndex> indices = new ArrayList<>();
        if (query.isSearchAllFiles()) {
            for (LogFileMeta meta : context.getFileService().getOpenFiles()) {
                LargeFileIndex idx = context.getFileService().getIndex(meta.getFilePath());
                if (idx != null) indices.add(idx);
            }
        } else {
            LargeFileIndex current = context.getCurrentIndex();
            if (current != null) indices.add(current);
        }

        if (indices.isEmpty()) {
            lblMatchStats.setText("No log files open to search.");
            return;
        }

        searchResults.clear();
        progressSearch.setVisible(true);
        btnCancelSearch.setDisable(false);
        lblMatchStats.setText("Searching...");

        context.getSearchService().executeSearchAsync(
                indices,
                query,
                match -> Platform.runLater(() -> {
                    searchResults.add(match);
                    lblMatchStats.setText(searchResults.size() + " matches found");
                }),
                prog -> Platform.runLater(() -> progressSearch.setProgress(prog)),
                () -> Platform.runLater(() -> {
                    progressSearch.setVisible(false);
                    btnCancelSearch.setDisable(true);
                    lblMatchStats.setText("Search completed: " + searchResults.size() + " matches found.");
                })
        );
    }

    @FXML
    private void onCancelSearch() {
        context.getSearchService().cancelCurrentSearch();
        progressSearch.setVisible(false);
        btnCancelSearch.setDisable(true);
        lblMatchStats.setText("Search cancelled. Found " + searchResults.size() + " matches.");
    }
}
