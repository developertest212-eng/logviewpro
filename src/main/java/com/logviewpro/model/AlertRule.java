package com.logviewpro.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AlertRule {
    public enum AlertType {
        ERROR_THRESHOLD,      // e.g. > X errors in Y seconds
        EXCEPTION_MATCH,      // e.g. OutOfMemoryError, SQLException
        KEYWORD_MATCH,        // e.g. text or regex
        INACTIVITY_TIMEOUT    // e.g. no updates for X seconds
    }

    private long id;
    private final StringProperty name = new SimpleStringProperty("");
    private AlertType type = AlertType.KEYWORD_MATCH;
    private String parameter = ""; // threshold number, exception name, keyword pattern, or seconds
    private int threshold = 5;
    private int windowSeconds = 60;
    private final BooleanProperty enabled = new SimpleBooleanProperty(false); // disabled by default per specs

    // Actions
    private boolean notifyDesktop = true;
    private boolean playSound = true;
    private boolean highlightEntry = true;
    private boolean recordHistory = true;

    public AlertRule() {}

    public AlertRule(String name, AlertType type, String parameter) {
        this.name.set(name);
        this.type = type;
        this.parameter = parameter;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public boolean isNotifyDesktop() {
        return notifyDesktop;
    }

    public void setNotifyDesktop(boolean notifyDesktop) {
        this.notifyDesktop = notifyDesktop;
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    public boolean isHighlightEntry() {
        return highlightEntry;
    }

    public void setHighlightEntry(boolean highlightEntry) {
        this.highlightEntry = highlightEntry;
    }

    public boolean isRecordHistory() {
        return recordHistory;
    }

    public void setRecordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }
}
