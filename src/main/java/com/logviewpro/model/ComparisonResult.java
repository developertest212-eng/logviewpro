package com.logviewpro.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ComparisonResult {
    private String fileAPath;
    private String fileBPath;
    private LocalDateTime comparisonTime = LocalDateTime.now();
    private boolean ignoredTimestamps;

    private int totalA;
    private int totalB;
    private int identicalCount;
    private int addedCount;
    private int removedCount;
    private int modifiedCount;

    private int errorsA;
    private int errorsB;
    private int warningsA;
    private int warningsB;

    private final List<DiffEntry> diffEntries = new ArrayList<>();

    public ComparisonResult() {}

    public String getFileAPath() {
        return fileAPath;
    }

    public void setFileAPath(String fileAPath) {
        this.fileAPath = fileAPath;
    }

    public String getFileBPath() {
        return fileBPath;
    }

    public void setFileBPath(String fileBPath) {
        this.fileBPath = fileBPath;
    }

    public LocalDateTime getComparisonTime() {
        return comparisonTime;
    }

    public void setComparisonTime(LocalDateTime comparisonTime) {
        this.comparisonTime = comparisonTime;
    }

    public boolean isIgnoredTimestamps() {
        return ignoredTimestamps;
    }

    public void setIgnoredTimestamps(boolean ignoredTimestamps) {
        this.ignoredTimestamps = ignoredTimestamps;
    }

    public int getTotalA() {
        return totalA;
    }

    public void setTotalA(int totalA) {
        this.totalA = totalA;
    }

    public int getTotalB() {
        return totalB;
    }

    public void setTotalB(int totalB) {
        this.totalB = totalB;
    }

    public int getIdenticalCount() {
        return identicalCount;
    }

    public void setIdenticalCount(int identicalCount) {
        this.identicalCount = identicalCount;
    }

    public int getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(int addedCount) {
        this.addedCount = addedCount;
    }

    public int getRemovedCount() {
        return removedCount;
    }

    public void setRemovedCount(int removedCount) {
        this.removedCount = removedCount;
    }

    public int getModifiedCount() {
        return modifiedCount;
    }

    public void setModifiedCount(int modifiedCount) {
        this.modifiedCount = modifiedCount;
    }

    public int getErrorsA() {
        return errorsA;
    }

    public void setErrorsA(int errorsA) {
        this.errorsA = errorsA;
    }

    public int getErrorsB() {
        return errorsB;
    }

    public void setErrorsB(int errorsB) {
        this.errorsB = errorsB;
    }

    public int getWarningsA() {
        return warningsA;
    }

    public void setWarningsA(int warningsA) {
        this.warningsA = warningsA;
    }

    public int getWarningsB() {
        return warningsB;
    }

    public void setWarningsB(int warningsB) {
        this.warningsB = warningsB;
    }

    public List<DiffEntry> getDiffEntries() {
        return diffEntries;
    }

    public void addDiffEntry(DiffEntry entry) {
        if (entry != null) {
            diffEntries.add(entry);
        }
    }
}
