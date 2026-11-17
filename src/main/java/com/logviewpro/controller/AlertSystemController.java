package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.model.AlertNotification;
import com.logviewpro.model.AlertRule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class AlertSystemController {

    @FXML private TextField txtRuleName;
    @FXML private ComboBox<AlertRule.AlertType> comboAlertType;
    @FXML private TextField txtParameter;
    @FXML private TextField txtThreshold;
    @FXML private TextField txtWindowSec;
    @FXML private CheckBox chkNotifyDesktop;
    @FXML private CheckBox chkPlaySound;
    @FXML private CheckBox chkHighlightEntry;
    @FXML private Button btnCreateRule;

    @FXML private TableView<AlertRule> tableRules;
    @FXML private TableColumn<AlertRule, Boolean> colRuleEnabled;
    @FXML private TableColumn<AlertRule, String> colRuleName;
    @FXML private TableColumn<AlertRule, String> colRuleType;
    @FXML private TableColumn<AlertRule, String> colRuleParam;
    @FXML private TableColumn<AlertRule, String> colRuleActions;

    @FXML private ListView<String> listNotifications;
    @FXML private Button btnClearHistory;
    @FXML private Button btnDeleteRule;

    private final AppContext context = AppContext.getInstance();
    private final ObservableList<AlertRule> rulesList = FXCollections.observableArrayList();
    private final ObservableList<String> notificationList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        comboAlertType.getItems().addAll(AlertRule.AlertType.values());
        comboAlertType.setValue(AlertRule.AlertType.KEYWORD_MATCH);

        colRuleEnabled.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        colRuleEnabled.setCellFactory(col -> new TableCell<>() {
            private final CheckBox chk = new CheckBox();
            {
                chk.setOnAction(e -> {
                    AlertRule rule = getTableView().getItems().get(getIndex());
                    rule.setEnabled(chk.isSelected());
                    context.getAlertService().updateRule(rule);
                });
            }
            @Override
            protected void updateItem(Boolean enabled, boolean empty) {
                super.updateItem(enabled, empty);
                if (empty || enabled == null) {
                    setGraphic(null);
                } else {
                    chk.setSelected(enabled);
                    setGraphic(chk);
                }
            }
        });

        colRuleName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRuleType.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType().name()));
        colRuleParam.setCellValueFactory(new PropertyValueFactory<>("parameter"));
        colRuleActions.setCellValueFactory(cellData -> {
            AlertRule r = cellData.getValue();
            StringBuilder sb = new StringBuilder();
            if (r.isNotifyDesktop()) sb.append("Desktop ");
            if (r.isPlaySound()) sb.append("Sound ");
            if (r.isHighlightEntry()) sb.append("Highlight");
            return new javafx.beans.property.SimpleStringProperty(sb.toString());
        });

        tableRules.setItems(rulesList);
        listNotifications.setItems(notificationList);

        btnCreateRule.setOnAction(e -> onCreateRule());
        btnClearHistory.setOnAction(e -> {
            context.getDbService().clearAlertHistory();
            refreshHistory();
        });
        btnDeleteRule.setOnAction(e -> {
            AlertRule selected = tableRules.getSelectionModel().getSelectedItem();
            if (selected != null) {
                context.getAlertService().deleteRule(selected);
                refreshRules();
            }
        });

        refreshRules();
        refreshHistory();
    }

    private void refreshRules() {
        rulesList.setAll(context.getAlertService().getRules());
    }

    public void refreshHistory() {
        notificationList.clear();
        List<AlertNotification> history = context.getDbService().getAlertHistory(50);
        for (AlertNotification n : history) {
            notificationList.add(String.format("[%s] %s: %s (File: %s, Line: %d)",
                    n.getTimestamp(), n.getTitle(), n.getMessage(), n.getSourceFile(), n.getLineNumber()));
        }
    }

    private void onCreateRule() {
        String name = txtRuleName.getText() != null ? txtRuleName.getText().trim() : "";
        if (name.isEmpty()) return;

        AlertRule rule = new AlertRule(name, comboAlertType.getValue(), txtParameter.getText());
        try {
            if (!txtThreshold.getText().isBlank()) rule.setThreshold(Integer.parseInt(txtThreshold.getText().trim()));
            if (!txtWindowSec.getText().isBlank()) rule.setWindowSeconds(Integer.parseInt(txtWindowSec.getText().trim()));
        } catch (NumberFormatException ignored) {}

        rule.setNotifyDesktop(chkNotifyDesktop.isSelected());
        rule.setPlaySound(chkPlaySound.isSelected());
        rule.setHighlightEntry(chkHighlightEntry.isSelected());
        rule.setEnabled(false); // Default to disabled per specs

        context.getAlertService().addRule(rule);
        refreshRules();

        txtRuleName.clear();
        txtParameter.clear();
    }
}
