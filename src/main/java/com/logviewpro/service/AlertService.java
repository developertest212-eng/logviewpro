package com.logviewpro.service;

import com.logviewpro.model.AlertNotification;
import com.logviewpro.model.AlertRule;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class AlertService {
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    private final DatabaseService dbService;
    private final List<AlertRule> rules = new CopyOnWriteArrayList<>();
    private final List<AlertNotification> recentNotifications = new CopyOnWriteArrayList<>();

    // Rolling window error timestamps for threshold rule
    private final Deque<Long> errorTimestamps = new ArrayDeque<>();
    private final Map<String, Long> lastFileActivity = new ConcurrentHashMap<>();

    public AlertService(DatabaseService dbService) {
        this.dbService = dbService;
        reloadRules();
    }

    public void reloadRules() {
        rules.clear();
        rules.addAll(dbService.getAllAlertRules());
    }

    public List<AlertRule> getRules() {
        return rules;
    }

    public void addRule(AlertRule rule) {
        dbService.saveAlertRule(rule);
        rules.add(rule);
    }

    public void updateRule(AlertRule rule) {
        dbService.updateAlertRule(rule);
    }

    public void deleteRule(AlertRule rule) {
        dbService.deleteAlertRule(rule.getId());
        rules.remove(rule);
    }

    public List<AlertNotification> getRecentNotifications() {
        return recentNotifications;
    }

    /**
     * Evaluates all enabled alert rules against an incoming log entry.
     */
    public void evaluateEntry(LogEntry entry) {
        if (entry == null) return;

        long now = System.currentTimeMillis();
        if (entry.getFilePath() != null) {
            lastFileActivity.put(entry.getFilePath(), now);
        }

        boolean isError = entry.getLevel() == LogLevel.ERROR || entry.getLevel() == LogLevel.FATAL;
        if (isError) {
            synchronized (errorTimestamps) {
                errorTimestamps.addLast(now);
                // Purge older than 5 minutes
                while (!errorTimestamps.isEmpty() && now - errorTimestamps.peekFirst() > 300_000) {
                    errorTimestamps.pollFirst();
                }
            }
        }

        for (AlertRule rule : rules) {
            if (!rule.isEnabled()) continue;

            boolean triggered = false;
            String alertTitle = "LogView Pro Alert: " + rule.getName();
            String alertMsg = "";

            switch (rule.getType()) {
                case ERROR_THRESHOLD -> {
                    if (isError) {
                        int windowMs = rule.getWindowSeconds() * 1000;
                        int count;
                        synchronized (errorTimestamps) {
                            count = (int) errorTimestamps.stream()
                                    .filter(t -> now - t <= windowMs)
                                    .count();
                        }
                        if (count >= rule.getThreshold()) {
                            triggered = true;
                            alertMsg = String.format("Error threshold reached: %d errors in %d seconds", count, rule.getWindowSeconds());
                        }
                    }
                }
                case EXCEPTION_MATCH -> {
                    String param = rule.getParameter().toLowerCase();
                    String fullText = (entry.getMessage() + "\n" + entry.getStackTrace()).toLowerCase();
                    if (fullText.contains(param)) {
                        triggered = true;
                        alertMsg = "Target exception matched: " + rule.getParameter() + " at line " + entry.getLineNumber();
                    }
                }
                case KEYWORD_MATCH -> {
                    String param = rule.getParameter();
                    String fullText = entry.getMessage() + "\n" + entry.getStackTrace();
                    try {
                        if (Pattern.compile(param, Pattern.CASE_INSENSITIVE).matcher(fullText).find()) {
                            triggered = true;
                            alertMsg = "Keyword pattern matched: '" + param + "' in message";
                        }
                    } catch (Exception e) {
                        if (fullText.toLowerCase().contains(param.toLowerCase())) {
                            triggered = true;
                            alertMsg = "Keyword matched: '" + param + "' in message";
                        }
                    }
                }
                case INACTIVITY_TIMEOUT -> {
                    // Handled in separate timer task if needed
                }
            }

            if (triggered) {
                fireAlert(rule, alertTitle, alertMsg, entry);
            }
        }
    }

    private void fireAlert(AlertRule rule, String title, String message, LogEntry entry) {
        if (rule.isHighlightEntry() && entry != null) {
            entry.setHighlighted(true);
        }

        if (rule.isPlaySound()) {
            SystemUtil.playNotificationSound();
        }

        if (rule.isNotifyDesktop()) {
            SystemUtil.showDesktopNotification(title, message, TrayIcon.MessageType.WARNING);
        }

        if (rule.isRecordHistory()) {
            AlertNotification notification = new AlertNotification(
                    rule.getName(),
                    title,
                    message,
                    entry != null ? entry.getFilePath() : "Unknown",
                    entry != null ? entry.getLineNumber() : 0
            );
            recentNotifications.add(0, notification);
            dbService.saveAlertNotification(notification);
        }
    }
}
