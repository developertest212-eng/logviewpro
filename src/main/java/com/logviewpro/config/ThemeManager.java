package com.logviewpro.config;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    private static final String DARK_THEME_CSS = "/css/dark-theme.css";
    private static final String LIGHT_THEME_CSS = "/css/light-theme.css";

    public static void applyTheme(Scene scene, String themeName) {
        if (scene == null) return;
        scene.getStylesheets().clear();

        String cssPath = "light".equalsIgnoreCase(themeName) ? LIGHT_THEME_CSS : DARK_THEME_CSS;
        URL resource = ThemeManager.class.getResource(cssPath);
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
            logger.info("Applied stylesheet: {}", cssPath);
        } else {
            logger.warn("Stylesheet not found: {}", cssPath);
        }
    }
}
