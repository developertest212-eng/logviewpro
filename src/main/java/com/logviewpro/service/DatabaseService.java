package com.logviewpro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logviewpro.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String dbUrl;

    public DatabaseService() {
        String userHome = System.getProperty("user.home");
        File dir = new File(userHome, ".logviewpro");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dbFile = new File(dir, "logviewpro.db");
        this.dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        initDatabase();
    }

    public DatabaseService(String customDbPath) {
        this.dbUrl = "jdbc:sqlite:" + customDbPath;
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            // Recent / Pinned files
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS file_history (
                    file_path TEXT PRIMARY KEY,
                    file_name TEXT,
                    last_opened TEXT,
                    is_pinned INTEGER DEFAULT 0
                )
            """);

            // Saved filters
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS saved_filters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT,
                    logic TEXT,
                    exclude INTEGER DEFAULT 0,
                    conditions_json TEXT,
                    created_at TEXT
                )
            """);

            // Saved searches
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS search_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    search_text TEXT,
                    is_saved INTEGER DEFAULT 0,
                    query_json TEXT,
                    created_at TEXT
                )
            """);

            // Alert rules
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS alert_rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT,
                    type TEXT,
                    parameter TEXT,
                    threshold INTEGER DEFAULT 5,
                    window_seconds INTEGER DEFAULT 60,
                    enabled INTEGER DEFAULT 0,
                    notify_desktop INTEGER DEFAULT 1,
                    play_sound INTEGER DEFAULT 1,
                    highlight_entry INTEGER DEFAULT 1,
                    record_history INTEGER DEFAULT 1
                )
            """);

            // Alert history
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS alert_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rule_name TEXT,
                    title TEXT,
                    message TEXT,
                    source_file TEXT,
                    line_number INTEGER,
                    created_at TEXT,
                    is_read INTEGER DEFAULT 0
                )
            """);

            // Settings key-value
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT
                )
            """);

            logger.info("SQLite database initialized successfully at {}", dbUrl);
        } catch (SQLException e) {
            logger.error("Failed to initialize SQLite database: {}", e.getMessage(), e);
        }
    }

    // ==================== File History ====================

    public void recordFileOpened(String filePath, String fileName) {
        String sql = """
            INSERT INTO file_history (file_path, file_name, last_opened, is_pinned)
            VALUES (?, ?, ?, 0)
            ON CONFLICT(file_path) DO UPDATE SET
                file_name = excluded.file_name,
                last_opened = excluded.last_opened
        """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filePath);
            ps.setString(2, fileName);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error saving file history: {}", e.getMessage());
        }
    }

    public void updatePinnedStatus(String filePath, boolean isPinned) {
        String sql = "UPDATE file_history SET is_pinned = ? WHERE file_path = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isPinned ? 1 : 0);
            ps.setString(2, filePath);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating pinned status: {}", e.getMessage());
        }
    }

    public List<LogFileMeta> getRecentFiles(int limit) {
        List<LogFileMeta> list = new ArrayList<>();
        String sql = "SELECT file_path, file_name, is_pinned FROM file_history ORDER BY is_pinned DESC, last_opened DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    File file = new File(rs.getString("file_path"));
                    LogFileMeta meta = new LogFileMeta(file);
                    meta.setFilePath(rs.getString("file_path"));
                    meta.setFileName(rs.getString("file_name"));
                    meta.setPinned(rs.getInt("is_pinned") == 1);
                    list.add(meta);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching recent files: {}", e.getMessage());
        }
        return list;
    }

    // ==================== Saved Filters ====================

    public void saveFilter(LogFilter filter) {
        String sql = """
            INSERT INTO saved_filters (name, logic, exclude, conditions_json, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, filter.getName());
            ps.setString(2, filter.getLogic().name());
            ps.setInt(3, filter.isExclude() ? 1 : 0);
            ps.setString(4, MAPPER.writeValueAsString(filter.getConditions()));
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    filter.setId(generatedKeys.getLong(1));
                }
            }
        } catch (Exception e) {
            logger.error("Error saving filter: {}", e.getMessage());
        }
    }

    public List<LogFilter> getAllFilters() {
        List<LogFilter> list = new ArrayList<>();
        String sql = "SELECT id, name, logic, exclude, conditions_json FROM saved_filters ORDER BY id ASC";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LogFilter filter = new LogFilter(rs.getString("name"));
                filter.setId(rs.getLong("id"));
                filter.setLogic(LogFilter.Logic.valueOf(rs.getString("logic")));
                filter.setExclude(rs.getInt("exclude") == 1);
                String json = rs.getString("conditions_json");
                if (json != null && !json.isBlank()) {
                    List<FilterCondition> conds = MAPPER.readValue(json, new TypeReference<List<FilterCondition>>() {});
                    filter.getConditions().addAll(conds);
                }
                list.add(filter);
            }
        } catch (Exception e) {
            logger.error("Error loading filters: {}", e.getMessage());
        }
        return list;
    }

    public void deleteFilter(long id) {
        String sql = "DELETE FROM saved_filters WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting filter: {}", e.getMessage());
        }
    }

    // ==================== Alert Rules ====================

    public void saveAlertRule(AlertRule rule) {
        String sql = """
            INSERT INTO alert_rules (name, type, parameter, threshold, window_seconds, enabled, notify_desktop, play_sound, highlight_entry, record_history)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rule.getName());
            ps.setString(2, rule.getType().name());
            ps.setString(3, rule.getParameter());
            ps.setInt(4, rule.getThreshold());
            ps.setInt(5, rule.getWindowSeconds());
            ps.setInt(6, rule.isEnabled() ? 1 : 0);
            ps.setInt(7, rule.isNotifyDesktop() ? 1 : 0);
            ps.setInt(8, rule.isPlaySound() ? 1 : 0);
            ps.setInt(9, rule.isHighlightEntry() ? 1 : 0);
            ps.setInt(10, rule.isRecordHistory() ? 1 : 0);
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    rule.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving alert rule: {}", e.getMessage());
        }
    }

    public void updateAlertRule(AlertRule rule) {
        String sql = """
            UPDATE alert_rules SET
                name = ?, type = ?, parameter = ?, threshold = ?, window_seconds = ?,
                enabled = ?, notify_desktop = ?, play_sound = ?, highlight_entry = ?, record_history = ?
            WHERE id = ?
        """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rule.getName());
            ps.setString(2, rule.getType().name());
            ps.setString(3, rule.getParameter());
            ps.setInt(4, rule.getThreshold());
            ps.setInt(5, rule.getWindowSeconds());
            ps.setInt(6, rule.isEnabled() ? 1 : 0);
            ps.setInt(7, rule.isNotifyDesktop() ? 1 : 0);
            ps.setInt(8, rule.isPlaySound() ? 1 : 0);
            ps.setInt(9, rule.isHighlightEntry() ? 1 : 0);
            ps.setInt(10, rule.isRecordHistory() ? 1 : 0);
            ps.setLong(11, rule.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating alert rule: {}", e.getMessage());
        }
    }

    public List<AlertRule> getAllAlertRules() {
        List<AlertRule> list = new ArrayList<>();
        String sql = "SELECT * FROM alert_rules ORDER BY id ASC";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AlertRule rule = new AlertRule();
                rule.setId(rs.getLong("id"));
                rule.setName(rs.getString("name"));
                rule.setType(AlertRule.AlertType.valueOf(rs.getString("type")));
                rule.setParameter(rs.getString("parameter"));
                rule.setThreshold(rs.getInt("threshold"));
                rule.setWindowSeconds(rs.getInt("window_seconds"));
                rule.setEnabled(rs.getInt("enabled") == 1);
                rule.setNotifyDesktop(rs.getInt("notify_desktop") == 1);
                rule.setPlaySound(rs.getInt("play_sound") == 1);
                rule.setHighlightEntry(rs.getInt("highlight_entry") == 1);
                rule.setRecordHistory(rs.getInt("record_history") == 1);
                list.add(rule);
            }
        } catch (SQLException e) {
            logger.error("Error fetching alert rules: {}", e.getMessage());
        }
        return list;
    }

    public void deleteAlertRule(long id) {
        String sql = "DELETE FROM alert_rules WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting alert rule: {}", e.getMessage());
        }
    }

    // ==================== Alert History ====================

    public void saveAlertNotification(AlertNotification notification) {
        String sql = """
            INSERT INTO alert_history (rule_name, title, message, source_file, line_number, created_at, is_read)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, notification.getRuleName());
            ps.setString(2, notification.getTitle());
            ps.setString(3, notification.getMessage());
            ps.setString(4, notification.getSourceFile());
            ps.setLong(5, notification.getLineNumber());
            ps.setString(6, notification.getTimestamp() != null ? notification.getTimestamp().toString() : LocalDateTime.now().toString());
            ps.setInt(7, notification.isRead() ? 1 : 0);
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notification.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving alert notification: {}", e.getMessage());
        }
    }

    public List<AlertNotification> getAlertHistory(int limit) {
        List<AlertNotification> list = new ArrayList<>();
        String sql = "SELECT * FROM alert_history ORDER BY id DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AlertNotification n = new AlertNotification();
                    n.setId(rs.getLong("id"));
                    n.setRuleName(rs.getString("rule_name"));
                    n.setTitle(rs.getString("title"));
                    n.setMessage(rs.getString("message"));
                    n.setSourceFile(rs.getString("source_file"));
                    n.setLineNumber(rs.getLong("line_number"));
                    String ts = rs.getString("created_at");
                    if (ts != null) n.setTimestamp(LocalDateTime.parse(ts));
                    n.setRead(rs.getInt("is_read") == 1);
                    list.add(n);
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching alert history: {}", e.getMessage());
        }
        return list;
    }

    public void clearAlertHistory() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM alert_history");
        } catch (SQLException e) {
            logger.error("Error clearing alert history: {}", e.getMessage());
        }
    }

    // ==================== Settings Persistence ====================

    public void setSetting(String key, String value) {
        String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error saving setting {}: {}", key, e.getMessage());
        }
    }

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting setting {}: {}", key, e.getMessage());
        }
        return defaultValue;
    }
}
