package com.logviewpro.model;

import javafx.scene.paint.Color;

public enum LogLevel {
    FATAL("FATAL", 60, "#991B1B", "#FEE2E2", "#7F1D1D"),
    ERROR("ERROR", 50, "#EF4444", "#FEE2E2", "#991B1B"),
    WARN("WARN", 40, "#F59E0B", "#FEF3C7", "#92400E"),
    INFO("INFO", 30, "#3B82F6", "#DBEAFE", "#1E40AF"),
    DEBUG("DEBUG", 20, "#10B981", "#D1FAE5", "#065F46"),
    TRACE("TRACE", 10, "#6B7280", "#F3F4F6", "#374151"),
    UNKNOWN("UNKNOWN", 0, "#9CA3AF", "#F3F4F6", "#4B5563");

    private final String displayName;
    private final int severity;
    private final String hexColor;
    private final String lightBgColor;
    private final String darkBgColor;

    LogLevel(String displayName, int severity, String hexColor, String lightBgColor, String darkBgColor) {
        this.displayName = displayName;
        this.severity = severity;
        this.hexColor = hexColor;
        this.lightBgColor = lightBgColor;
        this.darkBgColor = darkBgColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSeverity() {
        return severity;
    }

    public String getHexColor() {
        return hexColor;
    }

    public String getLightBgColor() {
        return lightBgColor;
    }

    public String getDarkBgColor() {
        return darkBgColor;
    }

    public Color getFxColor() {
        return Color.web(hexColor);
    }

    public static LogLevel fromString(String text) {
        if (text == null || text.isBlank()) {
            return UNKNOWN;
        }
        String upper = text.trim().toUpperCase();
        return switch (upper) {
            case "FATAL", "CRITICAL", "EMERGENCY" -> FATAL;
            case "ERROR", "ERR", "SEVERE" -> ERROR;
            case "WARN", "WARNING" -> WARN;
            case "INFO", "NOTICE" -> INFO;
            case "DEBUG" -> DEBUG;
            case "TRACE", "FINE", "FINER", "FINEST" -> TRACE;
            default -> UNKNOWN;
        };
    }
}
