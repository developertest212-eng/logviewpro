package com.logviewpro.config;

import com.logviewpro.service.DatabaseService;
import javafx.beans.property.*;

public class AppConfig {
    private static AppConfig instance;

    private final DatabaseService dbService;

    private final StringProperty theme = new SimpleStringProperty("dark");
    private final StringProperty fontFamily = new SimpleStringProperty("Consolas");
    private final IntegerProperty fontSize = new SimpleIntegerProperty(13);
    private final IntegerProperty maxVisibleLines = new SimpleIntegerProperty(5000);
    private final BooleanProperty autoScroll = new BooleanPropertyBase(true) {
        @Override
        public Object getBean() { return AppConfig.this; }
        @Override
        public String getName() { return "autoScroll"; }
    };
    private final StringProperty defaultFolder = new SimpleStringProperty("");
    private final IntegerProperty recentFilesLimit = new SimpleIntegerProperty(15);
    private final StringProperty defaultEncoding = new SimpleStringProperty("UTF-8");
    private final StringProperty dateFormatPattern = new SimpleStringProperty("yyyy-MM-dd HH:mm:ss.SSS");
    private final BooleanProperty soundAlertsEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty desktopAlertsEnabled = new SimpleBooleanProperty(true);

    public AppConfig(DatabaseService dbService) {
        this.dbService = dbService;
        loadFromDatabase();
    }

    public static synchronized AppConfig getInstance(DatabaseService dbService) {
        if (instance == null) {
            instance = new AppConfig(dbService);
        }
        return instance;
    }

    public static synchronized AppConfig getInstance() {
        return instance;
    }

    public void loadFromDatabase() {
        if (dbService == null) return;
        theme.set(dbService.getSetting("theme", "dark"));
        fontFamily.set(dbService.getSetting("fontFamily", "Consolas"));
        fontSize.set(Integer.parseInt(dbService.getSetting("fontSize", "13")));
        maxVisibleLines.set(Integer.parseInt(dbService.getSetting("maxVisibleLines", "5000")));
        autoScroll.set(Boolean.parseBoolean(dbService.getSetting("autoScroll", "true")));
        defaultFolder.set(dbService.getSetting("defaultFolder", System.getProperty("user.home")));
        recentFilesLimit.set(Integer.parseInt(dbService.getSetting("recentFilesLimit", "15")));
        defaultEncoding.set(dbService.getSetting("defaultEncoding", "UTF-8"));
        dateFormatPattern.set(dbService.getSetting("dateFormatPattern", "yyyy-MM-dd HH:mm:ss.SSS"));
        soundAlertsEnabled.set(Boolean.parseBoolean(dbService.getSetting("soundAlertsEnabled", "true")));
        desktopAlertsEnabled.set(Boolean.parseBoolean(dbService.getSetting("desktopAlertsEnabled", "true")));
    }

    public void saveToDatabase() {
        if (dbService == null) return;
        dbService.setSetting("theme", theme.get());
        dbService.setSetting("fontFamily", fontFamily.get());
        dbService.setSetting("fontSize", String.valueOf(fontSize.get()));
        dbService.setSetting("maxVisibleLines", String.valueOf(maxVisibleLines.get()));
        dbService.setSetting("autoScroll", String.valueOf(autoScroll.get()));
        dbService.setSetting("defaultFolder", defaultFolder.get());
        dbService.setSetting("recentFilesLimit", String.valueOf(recentFilesLimit.get()));
        dbService.setSetting("defaultEncoding", defaultEncoding.get());
        dbService.setSetting("dateFormatPattern", dateFormatPattern.get());
        dbService.setSetting("soundAlertsEnabled", String.valueOf(soundAlertsEnabled.get()));
        dbService.setSetting("desktopAlertsEnabled", String.valueOf(desktopAlertsEnabled.get()));
    }

    public String getTheme() { return theme.get(); }
    public void setTheme(String theme) { this.theme.set(theme); }
    public StringProperty themeProperty() { return theme; }

    public String getFontFamily() { return fontFamily.get(); }
    public void setFontFamily(String fontFamily) { this.fontFamily.set(fontFamily); }
    public StringProperty fontFamilyProperty() { return fontFamily; }

    public int getFontSize() { return fontSize.get(); }
    public void setFontSize(int fontSize) { this.fontSize.set(fontSize); }
    public IntegerProperty fontSizeProperty() { return fontSize; }

    public int getMaxVisibleLines() { return maxVisibleLines.get(); }
    public void setMaxVisibleLines(int maxVisibleLines) { this.maxVisibleLines.set(maxVisibleLines); }
    public IntegerProperty maxVisibleLinesProperty() { return maxVisibleLines; }

    public boolean isAutoScroll() { return autoScroll.get(); }
    public void setAutoScroll(boolean autoScroll) { this.autoScroll.set(autoScroll); }
    public BooleanProperty autoScrollProperty() { return autoScroll; }

    public String getDefaultFolder() { return defaultFolder.get(); }
    public void setDefaultFolder(String defaultFolder) { this.defaultFolder.set(defaultFolder); }
    public StringProperty defaultFolderProperty() { return defaultFolder; }

    public int getRecentFilesLimit() { return recentFilesLimit.get(); }
    public void setRecentFilesLimit(int recentFilesLimit) { this.recentFilesLimit.set(recentFilesLimit); }
    public IntegerProperty recentFilesLimitProperty() { return recentFilesLimit; }

    public String getDefaultEncoding() { return defaultEncoding.get(); }
    public void setDefaultEncoding(String defaultEncoding) { this.defaultEncoding.set(defaultEncoding); }
    public StringProperty defaultEncodingProperty() { return defaultEncoding; }

    public String getDateFormatPattern() { return dateFormatPattern.get(); }
    public void setDateFormatPattern(String dateFormatPattern) { this.dateFormatPattern.set(dateFormatPattern); }
    public StringProperty dateFormatPatternProperty() { return dateFormatPattern; }

    public boolean isSoundAlertsEnabled() { return soundAlertsEnabled.get(); }
    public void setSoundAlertsEnabled(boolean soundAlertsEnabled) { this.soundAlertsEnabled.set(soundAlertsEnabled); }
    public BooleanProperty soundAlertsEnabledProperty() { return soundAlertsEnabled; }

    public boolean isDesktopAlertsEnabled() { return desktopAlertsEnabled.get(); }
    public void setDesktopAlertsEnabled(boolean desktopAlertsEnabled) { this.desktopAlertsEnabled.set(desktopAlertsEnabled); }
    public BooleanProperty desktopAlertsEnabledProperty() { return desktopAlertsEnabled; }
}
