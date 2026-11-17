package com.logviewpro.model;

public enum FilterField {
    LEVEL("Log Level"),
    MESSAGE("Message"),
    LOGGER("Logger / Class"),
    THREAD("Thread"),
    TIMESTAMP("Timestamp"),
    EXCEPTION("Exception");

    private final String label;

    FilterField(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
