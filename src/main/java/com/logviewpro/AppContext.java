package com.logviewpro;

import com.logviewpro.config.AppConfig;
import com.logviewpro.index.LargeFileIndex;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogFileMeta;
import com.logviewpro.service.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AppContext {
    private static AppContext instance;

    private final DatabaseService dbService;
    private final LogFileService fileService;
    private final LogMonitorService monitorService;
    private final LogSearchService searchService;
    private final ExceptionAnalyzerService exceptionService;
    private final LogAnalyticsService analyticsService;
    private final LogExportService exportService;
    private final AlertService alertService;
    private final ComparisonService comparisonService;
    private final AppConfig appConfig;

    private final ObjectProperty<LogFileMeta> currentFileMeta = new SimpleObjectProperty<>();
    private final ObjectProperty<LargeFileIndex> currentIndex = new SimpleObjectProperty<>();
    private final ObservableList<LogEntry> currentVisibleEntries = FXCollections.observableArrayList();

    private AppContext() {
        this.dbService = new DatabaseService();
        this.appConfig = AppConfig.getInstance(dbService);
        this.fileService = new LogFileService(dbService);
        this.monitorService = new LogMonitorService();
        this.searchService = new LogSearchService();
        this.exceptionService = new ExceptionAnalyzerService();
        this.analyticsService = new LogAnalyticsService();
        this.exportService = new LogExportService();
        this.alertService = new AlertService(dbService);
        this.comparisonService = new ComparisonService();

        // Forward live monitor entries to alert evaluator
        this.monitorService.addListener(new com.logviewpro.monitor.TailerListener() {
            @Override
            public void onNewEntries(String filePath, java.util.List<LogEntry> entries) {
                for (LogEntry e : entries) {
                    alertService.evaluateEntry(e);
                }
            }
            @Override public void onFileRotated(String filePath) {}
            @Override public void onFileTruncated(String filePath) {}
            @Override public void onStatusChanged(String filePath, boolean active, String message) {}
            @Override public void onError(String filePath, Throwable throwable) {}
        });
    }

    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    public DatabaseService getDbService() { return dbService; }
    public LogFileService getFileService() { return fileService; }
    public LogMonitorService getMonitorService() { return monitorService; }
    public LogSearchService getSearchService() { return searchService; }
    public ExceptionAnalyzerService getExceptionService() { return exceptionService; }
    public LogAnalyticsService getAnalyticsService() { return analyticsService; }
    public LogExportService getExportService() { return exportService; }
    public AlertService getAlertService() { return alertService; }
    public ComparisonService getComparisonService() { return comparisonService; }
    public AppConfig getAppConfig() { return appConfig; }

    public LogFileMeta getCurrentFileMeta() { return currentFileMeta.get(); }
    public void setCurrentFileMeta(LogFileMeta meta) { this.currentFileMeta.set(meta); }
    public ObjectProperty<LogFileMeta> currentFileMetaProperty() { return currentFileMeta; }

    public LargeFileIndex getCurrentIndex() { return currentIndex.get(); }
    public void setCurrentIndex(LargeFileIndex index) { this.currentIndex.set(index); }
    public ObjectProperty<LargeFileIndex> currentIndexProperty() { return currentIndex; }

    public ObservableList<LogEntry> getCurrentVisibleEntries() { return currentVisibleEntries; }
}
