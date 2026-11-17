package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.config.AppConfig;
import com.logviewpro.config.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class SettingsController {

    @FXML private ComboBox<String> comboTheme;
    @FXML private ComboBox<String> comboFontFamily;
    @FXML private Spinner<Integer> spinFontSize;
    @FXML private Spinner<Integer> spinMaxVisibleLines;
    @FXML private Spinner<Integer> spinRecentFilesLimit;
    @FXML private CheckBox chkAutoScroll;
    @FXML private TextField txtDefaultFolder;
    @FXML private ComboBox<String> comboDefaultEncoding;
    @FXML private TextField txtDateFormatPattern;
    @FXML private CheckBox chkSoundAlerts;
    @FXML private CheckBox chkDesktopAlerts;

    @FXML private Button btnBrowseFolder;
    @FXML private Button btnSaveSettings;
    @FXML private Label lblSettingsStatus;

    private final AppContext context = AppContext.getInstance();
    private final AppConfig config = context.getAppConfig();

    @FXML
    public void initialize() {
        comboTheme.getItems().addAll("Dark", "Light");
        comboTheme.setValue("dark".equalsIgnoreCase(config.getTheme()) ? "Dark" : "Light");

        comboFontFamily.getItems().addAll("Consolas", "Courier New", "Monospaced", "Segoe UI");
        comboFontFamily.setValue(config.getFontFamily());

        spinFontSize.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 24, config.getFontSize()));
        spinMaxVisibleLines.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 50000, config.getMaxVisibleLines(), 500));
        spinRecentFilesLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 50, config.getRecentFilesLimit()));

        chkAutoScroll.setSelected(config.isAutoScroll());
        txtDefaultFolder.setText(config.getDefaultFolder());

        comboDefaultEncoding.getItems().addAll("UTF-8", "UTF-16LE", "UTF-16BE", "windows-1252", "ISO-8859-1");
        comboDefaultEncoding.setValue(config.getDefaultEncoding());

        txtDateFormatPattern.setText(config.getDateFormatPattern());
        chkSoundAlerts.setSelected(config.isSoundAlertsEnabled());
        chkDesktopAlerts.setSelected(config.isDesktopAlertsEnabled());

        btnBrowseFolder.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Default Log Directory");
            File f = dc.showDialog(btnBrowseFolder.getScene().getWindow());
            if (f != null) {
                txtDefaultFolder.setText(f.getAbsolutePath());
            }
        });

        btnSaveSettings.setOnAction(e -> onSaveSettings());
    }

    private void onSaveSettings() {
        String chosenTheme = comboTheme.getValue().toLowerCase();
        config.setTheme(chosenTheme);
        config.setFontFamily(comboFontFamily.getValue());
        config.setFontSize(spinFontSize.getValue());
        config.setMaxVisibleLines(spinMaxVisibleLines.getValue());
        config.setRecentFilesLimit(spinRecentFilesLimit.getValue());
        config.setAutoScroll(chkAutoScroll.isSelected());
        config.setDefaultFolder(txtDefaultFolder.getText());
        config.setDefaultEncoding(comboDefaultEncoding.getValue());
        config.setDateFormatPattern(txtDateFormatPattern.getText());
        config.setSoundAlertsEnabled(chkSoundAlerts.isSelected());
        config.setDesktopAlertsEnabled(chkDesktopAlerts.isSelected());

        config.saveToDatabase();

        // Apply theme immediately
        ThemeManager.applyTheme(comboTheme.getScene(), chosenTheme);

        lblSettingsStatus.setText("Settings saved and applied successfully!");
    }
}
