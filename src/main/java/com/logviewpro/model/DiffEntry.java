package com.logviewpro.model;

public class DiffEntry {
    public enum DiffType {
        IDENTICAL,
        ADDED,     // Present in File B but not in File A
        REMOVED,   // Present in File A but not in File B
        MODIFIED   // Similar pattern but changed details/level
    }

    private final DiffType type;
    private final LogEntry entryA; // can be null for ADDED
    private final LogEntry entryB; // can be null for REMOVED
    private final String description;

    public DiffEntry(DiffType type, LogEntry entryA, LogEntry entryB, String description) {
        this.type = type;
        this.entryA = entryA;
        this.entryB = entryB;
        this.description = description;
    }

    public DiffType getType() {
        return type;
    }

    public LogEntry getEntryA() {
        return entryA;
    }

    public LogEntry getEntryB() {
        return entryB;
    }

    public String getDescription() {
        return description;
    }
}
