package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.config.ThemeManager;
import com.logviewpro.model.LogFileMeta;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private BorderPane rootPane;
    @FXML private StackPane contentArea;

    // Top Bar
    @FXML private Label lblActiveFileName;
    @FXML private Label lblActiveFileBadge;
    @FXML private TextField txtGlobalSearch;
    @FXML private Button btnThemeToggle;
    @FXML private Button btnNotifications;
    @FXML private Button btnQuickOpenFile;

    // Sidebar navigation buttons
    @FXML private Button navDashboard;
    @FXML private Button navLiveMonitor;
    @FXML private Button navFileManagement;
    @FXML private Button navLogViewer;
    @FXML private Button navMultiFile;
    @FXML private Button navErrorAnalyzer;
    @FXML private Button navSearch;
    @FXML private Button navComparison;
    @FXML private Button navStatistics;
    @FXML private Button navFilters;
    @FXML private Button navAlerts;
    @FXML private Button navReports;
    @FXML private Button navSettings;

    private final AppContext context = AppContext.getInstance();
    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private Button currentActiveNavButton;

    @FXML
    public void initialize() {
        setupTopBar();
        setupNavigation();

        context.currentFileMetaProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateActiveFileDisplay(newVal));
        });

        // Load Dashboard by default
        navigateTo("Dashboard", navDashboard);
    }

    private void setupTopBar() {
        btnQuickOpenFile.setOnAction(e -> onQuickOpenFile());

        btnThemeToggle.setOnAction(e -> {
            String current = context.getAppConfig().getTheme();
            String next = "dark".equalsIgnoreCase(current) ? "light" : "dark";
            context.getAppConfig().setTheme(next);
            context.getAppConfig().saveToDatabase();
            ThemeManager.applyTheme(rootPane.getScene(), next);
            btnThemeToggle.setText("dark".equalsIgnoreCase(next) ? "🌙" : "☀️");
        });

        btnNotifications.setOnAction(e -> navigateTo("Alerts", navAlerts));

        txtGlobalSearch.setOnAction(e -> {
            String term = txtGlobalSearch.getText();
            if (term != null && !term.isBlank()) {
                navigateTo("Search", navSearch);
                SearchEngineController sec = (SearchEngineController) controllerCache.get("Search");
                if (sec != null) {
                    sec.setSearchTerm(term);
                }
            }
        });
    }

    private void setupNavigation() {
        navDashboard.setOnAction(e -> navigateTo("Dashboard", navDashboard));
        navLiveMonitor.setOnAction(e -> navigateTo("LiveMonitor", navLiveMonitor));
        navFileManagement.setOnAction(e -> navigateTo("FileManager", navFileManagement));
        navLogViewer.setOnAction(e -> navigateTo("LogViewer", navLogViewer));
        navMultiFile.setOnAction(e -> navigateTo("MultiFile", navMultiFile));
        navErrorAnalyzer.setOnAction(e -> navigateTo("ExceptionAnalyzer", navErrorAnalyzer));
        navSearch.setOnAction(e -> navigateTo("SearchEngine", navSearch));
        navComparison.setOnAction(e -> navigateTo("LogComparison", navComparison));
        navStatistics.setOnAction(e -> navigateTo("Analytics", navStatistics));
        navFilters.setOnAction(e -> navigateTo("FilterSystem", navFilters));
        navAlerts.setOnAction(e -> navigateTo("AlertSystem", navAlerts));
        navReports.setOnAction(e -> navigateTo("ReportExport", navReports));
        navSettings.setOnAction(e -> navigateTo("Settings", navSettings));
    }

    public void navigateTo(String viewName, Button navButton) {
        if (currentActiveNavButton != null) {
            currentActiveNavButton.getStyleClass().remove("nav-button-active");
        }
        if (navButton != null) {
            navButton.getStyleClass().add("nav-button-active");
            currentActiveNavButton = navButton;
        }

        Node node = viewCache.get(viewName);
        if (node == null) {
            try {
                String fxmlPath = "/fxml/" + viewName + "View.fxml";
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                node = loader.load();
                viewCache.put(viewName, node);
                controllerCache.put(viewName, loader.getController());
            } catch (IOException e) {
                logger.error("Failed to load view {}: {}", viewName, e.getMessage(), e);
                return;
            }
        }

        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void updateActiveFileDisplay(LogFileMeta meta) {
        if (meta != null) {
            lblActiveFileName.setText(meta.getFileName());
            lblActiveFileBadge.setText(meta.getFormattedSize() + " | " + meta.getLineCount() + " lines");
            lblActiveFileBadge.setVisible(true);
        } else {
            lblActiveFileName.setText("No file opened");
            lblActiveFileBadge.setText("");
            lblActiveFileBadge.setVisible(false);
        }
    }

    private void onQuickOpenFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Log File");
        File file = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            LogFileMeta meta = context.getFileService().openFile(file, null);
            if (meta != null) {
                context.setCurrentFileMeta(meta);
                context.setCurrentIndex(context.getFileService().getIndex(meta.getFilePath()));
                navigateTo("LogViewer", navLogViewer);
            }
        }
    }
}
