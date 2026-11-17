package com.logviewpro.model;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class FilterCondition {
    private FilterField field = FilterField.MESSAGE;
    private FilterOperator operator = FilterOperator.CONTAINS;
    private String value = "";
    private String secondaryValue = ""; // for BETWEEN operator
    private Pattern compiledPattern;

    public FilterCondition() {}

    public FilterCondition(FilterField field, FilterOperator operator, String value) {
        this.field = field;
        this.operator = operator;
        setValue(value);
    }

    public FilterField getField() {
        return field;
    }

    public void setField(FilterField field) {
        this.field = field;
    }

    public FilterOperator getOperator() {
        return operator;
    }

    public void setOperator(FilterOperator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value != null ? value : "";
        if (operator == FilterOperator.MATCHES_REGEX && !this.value.isEmpty()) {
            try {
                this.compiledPattern = Pattern.compile(this.value, Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                this.compiledPattern = null;
            }
        }
    }

    public String getSecondaryValue() {
        return secondaryValue;
    }

    public void setSecondaryValue(String secondaryValue) {
        this.secondaryValue = secondaryValue != null ? secondaryValue : "";
    }

    public boolean test(LogEntry entry) {
        if (entry == null) return false;
        String target = switch (field) {
            case LEVEL -> entry.getLevel().getDisplayName();
            case MESSAGE -> entry.getMessage();
            case LOGGER -> entry.getLogger();
            case THREAD -> entry.getThread();
            case TIMESTAMP -> entry.getRawTimestamp() != null ? entry.getRawTimestamp() : "";
            case EXCEPTION -> entry.getStackTrace();
        };

        if (field == FilterField.TIMESTAMP && entry.getTimestamp() != null) {
            return testTimestamp(entry.getTimestamp());
        }

        return testString(target);
    }

    private boolean testTimestamp(LocalDateTime entryTime) {
        try {
            if (operator == FilterOperator.AFTER && !value.isEmpty()) {
                LocalDateTime target = LocalDateTime.parse(value);
                return entryTime.isAfter(target);
            } else if (operator == FilterOperator.BEFORE && !value.isEmpty()) {
                LocalDateTime target = LocalDateTime.parse(value);
                return entryTime.isBefore(target);
            } else if (operator == FilterOperator.BETWEEN && !value.isEmpty() && !secondaryValue.isEmpty()) {
                LocalDateTime start = LocalDateTime.parse(value);
                LocalDateTime end = LocalDateTime.parse(secondaryValue);
                return !entryTime.isBefore(start) && !entryTime.isAfter(end);
            }
        } catch (Exception ignored) {
            // fallback to string test if parsing fails
        }
        return testString(entryTime.toString());
    }

    private boolean testString(String target) {
        if (target == null) target = "";
        String val = value != null ? value : "";

        return switch (operator) {
            case EQUALS -> target.equalsIgnoreCase(val);
            case NOT_EQUALS -> !target.equalsIgnoreCase(val);
            case CONTAINS -> target.toLowerCase().contains(val.toLowerCase());
            case NOT_CONTAINS -> !target.toLowerCase().contains(val.toLowerCase());
            case STARTS_WITH -> target.toLowerCase().startsWith(val.toLowerCase());
            case ENDS_WITH -> target.toLowerCase().endsWith(val.toLowerCase());
            case MATCHES_REGEX -> {
                if (compiledPattern != null) {
                    yield compiledPattern.matcher(target).find();
                } else {
                    yield target.toLowerCase().contains(val.toLowerCase());
                }
            }
            default -> target.toLowerCase().contains(val.toLowerCase());
        };
    }
}
