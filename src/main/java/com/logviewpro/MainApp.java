package com.logviewpro;

import com.logviewpro.config.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting LogView Pro Application...");
            AppContext context = AppContext.getInstance();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1340, 850);
            ThemeManager.applyTheme(scene, context.getAppConfig().getTheme());

            primaryStage.setTitle("LogView Pro – Advanced Log Analyzer & Real-Time Monitor");
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(700);
            primaryStage.setScene(scene);

            primaryStage.setOnCloseRequest(event -> {
                logger.info("Application shutdown requested. Stopping all background services...");
                context.getMonitorService().stopAll();
                context.getSearchService().cancelCurrentSearch();
                Platform.exit();
                System.exit(0);
            });

            primaryStage.show();
            logger.info("LogView Pro UI initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to start LogView Pro: {}", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
