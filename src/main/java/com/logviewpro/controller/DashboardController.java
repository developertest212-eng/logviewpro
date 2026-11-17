package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.model.LogAnalyticsData;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogFileMeta;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private Label lblTotalEntries;
    @FXML private Label lblErrorCount;
    @FXML private Label lblWarnCount;
    @FXML private Label lblInfoCount;
    @FXML private Label lblDebugCount;
    @FXML private Label lblFatalCount;
    @FXML private Label lblMonitoredFiles;
    @FXML private Label lblLastDetectedError;

    @FXML private LineChart<String, Number> chartLogsPerMinute;
    @FXML private BarChart<String, Number> chartHourlyActivity;
    @FXML private PieChart chartLevelDistribution;
    @FXML private BarChart<String, Number> chartTopExceptions;
    @FXML private VBox boxTopLoggers;

    private final AppContext context = AppContext.getInstance();

    @FXML
    public void initialize() {
        context.currentFileMetaProperty().addListener((obs, oldVal, newVal) -> refreshDashboard());
        refreshDashboard();
    }

    public void refreshDashboard() {
        Platform.runLater(() -> {
            LogFileMeta meta = context.getCurrentFileMeta();
            List<LogEntry> entries = context.getCurrentVisibleEntries();

            if (entries.isEmpty() && context.getCurrentIndex() != null) {
                // Read sample from index if available
                int count = Math.min(context.getCurrentIndex().size(), 5000);
                entries = new com.logviewpro.index.FileChunkReader().readEntriesRange(context.getCurrentIndex(), 0, count);
            }

            LogAnalyticsData data = context.getAnalyticsService().computeAnalytics(entries, null, null);

            // Update KPI cards
            lblTotalEntries.setText(meta != null ? String.format("%,d", meta.getLineCount()) : String.format("%,d", data.getTotalEntries()));
            lblErrorCount.setText(meta != null ? String.format("%,d", meta.getErrorCount()) : String.format("%,d", data.getTotalErrors()));
            lblWarnCount.setText(meta != null ? String.format("%,d", meta.getWarnCount()) : String.format("%,d", data.getTotalWarnings()));
            lblInfoCount.setText(meta != null ? String.format("%,d", meta.getInfoCount()) : String.format("%,d", data.getTotalInfo()));
            lblDebugCount.setText(meta != null ? String.format("%,d", meta.getDebugCount()) : String.format("%,d", data.getTotalDebug()));
            lblFatalCount.setText(meta != null ? String.format("%,d", meta.getFatalCount()) : String.format("%,d", data.getTotalFatal()));
            lblMonitoredFiles.setText(String.valueOf(context.getFileService().getOpenFiles().stream().filter(LogFileMeta::isMonitored).count()));

            String lastErr = data.getLastDetectedError();
            lblLastDetectedError.setText(lastErr != null && !lastErr.isBlank() ? lastErr : "No recent errors detected");

            // 1. Pie Chart - Log Level Distribution
            chartLevelDistribution.getData().clear();
            if (data.getTotalFatal() > 0) chartLevelDistribution.getData().add(new PieChart.Data("FATAL (" + data.getTotalFatal() + ")", data.getTotalFatal()));
            if (data.getTotalErrors() > 0) chartLevelDistribution.getData().add(new PieChart.Data("ERROR (" + data.getTotalErrors() + ")", data.getTotalErrors()));
            if (data.getTotalWarnings() > 0) chartLevelDistribution.getData().add(new PieChart.Data("WARN (" + data.getTotalWarnings() + ")", data.getTotalWarnings()));
            if (data.getTotalInfo() > 0) chartLevelDistribution.getData().add(new PieChart.Data("INFO (" + data.getTotalInfo() + ")", data.getTotalInfo()));
            if (data.getTotalDebug() > 0) chartLevelDistribution.getData().add(new PieChart.Data("DEBUG (" + data.getTotalDebug() + ")", data.getTotalDebug()));
            if (data.getTotalTrace() > 0) chartLevelDistribution.getData().add(new PieChart.Data("TRACE (" + data.getTotalTrace() + ")", data.getTotalTrace()));

            // 2. Line Chart - Logs Per Minute / Error Trends
            chartLogsPerMinute.getData().clear();
            XYChart.Series<String, Number> seriesAll = new XYChart.Series<>();
            seriesAll.setName("All Logs / min");
            data.getLogsPerMinute().forEach((min, cnt) -> seriesAll.getData().add(new XYChart.Data<>(min, cnt)));

            XYChart.Series<String, Number> seriesErr = new XYChart.Series<>();
            seriesErr.setName("Errors / min");
            data.getErrorTrends().forEach((min, cnt) -> seriesErr.getData().add(new XYChart.Data<>(min, cnt)));

            chartLogsPerMinute.getData().addAll(seriesAll, seriesErr);

            // 3. Bar Chart - Hourly Log Activity (0..23)
            chartHourlyActivity.getData().clear();
            XYChart.Series<String, Number> hourlySeries = new XYChart.Series<>();
            hourlySeries.setName("Logs");
            data.getLogsByHourOfDay().forEach((hour, cnt) -> {
                hourlySeries.getData().add(new XYChart.Data<>(String.format("%02d:00", hour), cnt));
            });
            chartHourlyActivity.getData().add(hourlySeries);

            // 4. Bar Chart - Most Frequent Exceptions
            chartTopExceptions.getData().clear();
            XYChart.Series<String, Number> exSeries = new XYChart.Series<>();
            exSeries.setName("Occurrences");
            List<Map.Entry<String, Long>> topEx = LogAnalyticsData.getTopN(data.getTopExceptions(), 6);
            for (Map.Entry<String, Long> entry : topEx) {
                exSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            chartTopExceptions.getData().add(exSeries);

            // 5. Top error-producing classes/loggers
            boxTopLoggers.getChildren().clear();
            List<Map.Entry<String, Long>> topLoggers = LogAnalyticsData.getTopN(data.getTopErrorProducingLoggers(), 5);
            if (topLoggers.isEmpty()) {
                Label empty = new Label("No error-producing loggers detected");
                empty.setStyle("-fx-text-fill: -color-text-muted; -fx-font-style: italic;");
                boxTopLoggers.getChildren().add(empty);
            } else {
                for (Map.Entry<String, Long> entry : topLoggers) {
                    Label lbl = new Label("• " + entry.getKey() + " (" + entry.getValue() + " errors)");
                    lbl.setStyle("-fx-text-fill: #FCA5A5; -fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
                    boxTopLoggers.getChildren().add(lbl);
                }
            }
        });
    }
}
