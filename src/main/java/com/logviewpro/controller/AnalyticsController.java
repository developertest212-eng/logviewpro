package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.index.FileChunkReader;
import com.logviewpro.model.LogAnalyticsData;
import com.logviewpro.model.LogEntry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AnalyticsController {

    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;
    @FXML private Button btnApplyDateFilter;
    @FXML private Button btnResetDateFilter;

    @FXML private Label lblAnalyticsTotal;
    @FXML private Label lblAnalyticsErrors;
    @FXML private Label lblAnalyticsWarnings;
    @FXML private Label lblAnalyticsErrorRate;

    @FXML private PieChart chartLogLevels;
    @FXML private LineChart<String, Number> chartErrorTrends;
    @FXML private BarChart<String, Number> chartHourlyLogs;
    @FXML private BarChart<String, Number> chartTopExceptions;

    @FXML private VBox boxTopThreads;
    @FXML private VBox boxTopLoggers;

    private final AppContext context = AppContext.getInstance();
    private final FileChunkReader chunkReader = new FileChunkReader();

    @FXML
    public void initialize() {
        btnApplyDateFilter.setOnAction(e -> refreshAnalytics());
        btnResetDateFilter.setOnAction(e -> {
            dpFromDate.setValue(null);
            dpToDate.setValue(null);
            refreshAnalytics();
        });

        refreshAnalytics();
    }

    public void refreshAnalytics() {
        new Thread(() -> {
            List<LogEntry> entries = context.getCurrentVisibleEntries();
            if (entries.isEmpty() && context.getCurrentIndex() != null) {
                int count = Math.min(context.getCurrentIndex().size(), 10000);
                entries = chunkReader.readEntriesRange(context.getCurrentIndex(), 0, count);
            }

            LocalDate from = dpFromDate.getValue();
            LocalDate to = dpToDate.getValue();
            LocalDateTime fromLdt = from != null ? from.atStartOfDay() : null;
            LocalDateTime toLdt = to != null ? to.atTime(23, 59, 59) : null;

            LogAnalyticsData data = context.getAnalyticsService().computeAnalytics(entries, fromLdt, toLdt);

            Platform.runLater(() -> {
                lblAnalyticsTotal.setText(String.format("%,d", data.getTotalEntries()));
                lblAnalyticsErrors.setText(String.format("%,d", data.getTotalErrors() + data.getTotalFatal()));
                lblAnalyticsWarnings.setText(String.format("%,d", data.getTotalWarnings()));
                lblAnalyticsErrorRate.setText(String.format("%.2f %%", data.getErrorPercentage()));

                // Pie Chart: Levels
                chartLogLevels.getData().clear();
                if (data.getTotalFatal() > 0) chartLogLevels.getData().add(new PieChart.Data("FATAL", data.getTotalFatal()));
                if (data.getTotalErrors() > 0) chartLogLevels.getData().add(new PieChart.Data("ERROR", data.getTotalErrors()));
                if (data.getTotalWarnings() > 0) chartLogLevels.getData().add(new PieChart.Data("WARN", data.getTotalWarnings()));
                if (data.getTotalInfo() > 0) chartLogLevels.getData().add(new PieChart.Data("INFO", data.getTotalInfo()));
                if (data.getTotalDebug() > 0) chartLogLevels.getData().add(new PieChart.Data("DEBUG", data.getTotalDebug()));
                if (data.getTotalTrace() > 0) chartLogLevels.getData().add(new PieChart.Data("TRACE", data.getTotalTrace()));

                // Line Chart: Error trends
                chartErrorTrends.getData().clear();
                XYChart.Series<String, Number> errSeries = new XYChart.Series<>();
                errSeries.setName("Errors");
                data.getErrorTrends().forEach((min, count) -> errSeries.getData().add(new XYChart.Data<>(min, count)));
                chartErrorTrends.getData().add(errSeries);

                // Bar Chart: Hourly activity
                chartHourlyLogs.getData().clear();
                XYChart.Series<String, Number> hourSeries = new XYChart.Series<>();
                hourSeries.setName("Hourly Activity");
                data.getLogsByHourOfDay().forEach((hr, count) -> hourSeries.getData().add(new XYChart.Data<>(String.format("%02d:00", hr), count)));
                chartHourlyLogs.getData().add(hourSeries);

                // Bar Chart: Top Exceptions
                chartTopExceptions.getData().clear();
                XYChart.Series<String, Number> exSeries = new XYChart.Series<>();
                exSeries.setName("Exceptions");
                for (Map.Entry<String, Long> entry : LogAnalyticsData.getTopN(data.getTopExceptions(), 6)) {
                    exSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                }
                chartTopExceptions.getData().add(exSeries);

                // Top Threads
                boxTopThreads.getChildren().clear();
                for (Map.Entry<String, Long> entry : LogAnalyticsData.getTopN(data.getTopThreads(), 5)) {
                    Label lbl = new Label("• " + entry.getKey() + ": " + entry.getValue() + " logs");
                    lbl.setStyle("-fx-text-fill: -color-text-main; -fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
                    boxTopThreads.getChildren().add(lbl);
                }

                // Top Loggers
                boxTopLoggers.getChildren().clear();
                for (Map.Entry<String, Long> entry : LogAnalyticsData.getTopN(data.getTopClasses(), 5)) {
                    Label lbl = new Label("• " + entry.getKey() + ": " + entry.getValue() + " logs");
                    lbl.setStyle("-fx-text-fill: -color-text-main; -fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
                    boxTopLoggers.getChildren().add(lbl);
                }
            });
        }).start();
    }
}
