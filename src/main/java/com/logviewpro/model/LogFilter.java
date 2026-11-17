package com.logviewpro.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.List;

public class LogFilter {
    public enum Logic { AND, OR }

    private long id;
    private final StringProperty name = new SimpleStringProperty("New Filter");
    private Logic logic = Logic.AND;
    private boolean exclude = false; // if true, invert match (Exclude mode)
    private final BooleanProperty active = new SimpleBooleanProperty(true);
    private final List<FilterCondition> conditions = new ArrayList<>();

    public LogFilter() {}

    public LogFilter(String name) {
        this.name.set(name);
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

    public Logic getLogic() {
        return logic;
    }

    public void setLogic(Logic logic) {
        this.logic = logic;
    }

    public boolean isExclude() {
        return exclude;
    }

    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    public boolean isActive() {
        return active.get();
    }

    public void setActive(boolean active) {
        this.active.set(active);
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    public List<FilterCondition> getConditions() {
        return conditions;
    }

    public void addCondition(FilterCondition condition) {
        if (condition != null) {
            conditions.add(condition);
        }
    }

    public boolean test(LogEntry entry) {
        if (!isActive()) return true;
        if (conditions.isEmpty()) return true;

        boolean matches;
        if (logic == Logic.AND) {
            matches = true;
            for (FilterCondition condition : conditions) {
                if (!condition.test(entry)) {
                    matches = false;
                    break;
                }
            }
        } else { // OR
            matches = false;
            for (FilterCondition condition : conditions) {
                if (condition.test(entry)) {
                    matches = true;
                    break;
                }
            }
        }

        return exclude ? !matches : matches;
    }

    @Override
    public String toString() {
        return getName() + " (" + conditions.size() + " rules, " + logic + ")";
    }
}
