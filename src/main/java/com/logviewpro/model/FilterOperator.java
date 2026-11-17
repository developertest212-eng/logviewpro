package com.logviewpro.model;

public enum FilterOperator {
    EQUALS("equals"),
    NOT_EQUALS("does not equal"),
    CONTAINS("contains"),
    NOT_CONTAINS("does not contain"),
    MATCHES_REGEX("matches regex"),
    STARTS_WITH("starts with"),
    ENDS_WITH("ends with"),
    BEFORE("before"),
    AFTER("after"),
    BETWEEN("between");

    private final String label;

    FilterOperator(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
