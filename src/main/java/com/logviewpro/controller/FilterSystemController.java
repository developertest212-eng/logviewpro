package com.logviewpro.controller;

import com.logviewpro.AppContext;
import com.logviewpro.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class FilterSystemController {

    @FXML private TextField txtFilterName;
    @FXML private ComboBox<LogFilter.Logic> comboLogic;
    @FXML private CheckBox chkExclude;
    @FXML private VBox boxConditionsList;
    @FXML private Button btnAddCondition;
    @FXML private Button btnSaveFilter;
    @FXML private Button btnResetFilter;

    @FXML private ListView<LogFilter> listSavedFilters;
    @FXML private Button btnApplyFilter;
    @FXML private Button btnDeleteFilter;

    @FXML private Label lblFilterStatus;

    private final AppContext context = AppContext.getInstance();
    private final ObservableList<LogFilter> savedFiltersList = FXCollections.observableArrayList();
    private final ObservableList<ConditionRow> currentConditions = FXCollections.observableArrayList();

    public static class ConditionRow {
        public ComboBox<FilterField> fieldCombo = new ComboBox<>(FXCollections.observableArrayList(FilterField.values()));
        public ComboBox<FilterOperator> opCombo = new ComboBox<>(FXCollections.observableArrayList(FilterOperator.values()));
        public TextField valField = new TextField();
        public Button removeBtn = new Button("✕");
        public HBox container = new HBox(8, fieldCombo, opCombo, valField, removeBtn);

        public ConditionRow() {
            fieldCombo.setValue(FilterField.MESSAGE);
            opCombo.setValue(FilterOperator.CONTAINS);
            valField.setPromptText("Value...");
            valField.setPrefWidth(220);
            removeBtn.getStyleClass().add("button-danger");
        }
    }

    @FXML
    public void initialize() {
        comboLogic.getItems().addAll(LogFilter.Logic.AND, LogFilter.Logic.OR);
        comboLogic.setValue(LogFilter.Logic.AND);

        listSavedFilters.setItems(savedFiltersList);

        btnAddCondition.setOnAction(e -> addConditionRow());
        btnSaveFilter.setOnAction(e -> onSaveFilter());
        btnResetFilter.setOnAction(e -> onResetFilter());
        btnApplyFilter.setOnAction(e -> onApplySelectedFilter());
        btnDeleteFilter.setOnAction(e -> onDeleteSelectedFilter());

        // Add 1 default condition
        addConditionRow();
        loadSavedFilters();
    }

    private void addConditionRow() {
        ConditionRow row = new ConditionRow();
        row.removeBtn.setOnAction(e -> {
            boxConditionsList.getChildren().remove(row.container);
            currentConditions.remove(row);
        });
        currentConditions.add(row);
        boxConditionsList.getChildren().add(row.container);
    }

    private void loadSavedFilters() {
        savedFiltersList.clear();
        List<LogFilter> filters = context.getDbService().getAllFilters();
        savedFiltersList.addAll(filters);
    }

    private void onSaveFilter() {
        String name = txtFilterName.getText() != null ? txtFilterName.getText().trim() : "";
        if (name.isEmpty()) {
            lblFilterStatus.setText("Please enter a filter name.");
            return;
        }

        LogFilter filter = new LogFilter(name);
        filter.setLogic(comboLogic.getValue());
        filter.setExclude(chkExclude.isSelected());

        for (ConditionRow row : currentConditions) {
            String val = row.valField.getText();
            if (val != null && !val.isBlank()) {
                filter.addCondition(new FilterCondition(row.fieldCombo.getValue(), row.opCombo.getValue(), val.trim()));
            }
        }

        context.getDbService().saveFilter(filter);
        loadSavedFilters();
        lblFilterStatus.setText("Filter '" + name + "' saved successfully!");
    }

    private void onResetFilter() {
        txtFilterName.clear();
        comboLogic.setValue(LogFilter.Logic.AND);
        chkExclude.setSelected(false);
        boxConditionsList.getChildren().clear();
        currentConditions.clear();
        addConditionRow();
        lblFilterStatus.setText("Filter builder reset.");
    }

    private void onApplySelectedFilter() {
        LogFilter selected = listSavedFilters.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblFilterStatus.setText("Select a filter to apply.");
            return;
        }

        lblFilterStatus.setText("Applied filter: " + selected.getName());
    }

    private void onDeleteSelectedFilter() {
        LogFilter selected = listSavedFilters.getSelectionModel().getSelectedItem();
        if (selected != null) {
            context.getDbService().deleteFilter(selected.getId());
            loadSavedFilters();
            lblFilterStatus.setText("Deleted filter.");
        }
    }
}
