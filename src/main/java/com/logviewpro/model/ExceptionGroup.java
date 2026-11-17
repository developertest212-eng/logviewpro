package com.logviewpro.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExceptionGroup {
    private String fingerprint;
    private String exceptionClass;
    private String simpleExceptionName;
    private String representativeMessage;
    private String topFrame; // e.g., com.example.UserService.getUserById(UserService.java:45)
    private String applicationFrame; // first non-JDK, non-framework frame
    private String fullStackTraceSample;
    private int count;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private final List<ExceptionOccurrence> occurrences = new ArrayList<>();

    public ExceptionGroup() {}

    public ExceptionGroup(String fingerprint, String exceptionClass, String representativeMessage) {
        this.fingerprint = fingerprint;
        this.exceptionClass = exceptionClass;
        this.simpleExceptionName = extractSimpleName(exceptionClass);
        this.representativeMessage = representativeMessage;
    }

    private static String extractSimpleName(String fqcn) {
        if (fqcn == null) return "UnknownException";
        int idx = fqcn.lastIndexOf('.');
        return idx >= 0 ? fqcn.substring(idx + 1) : fqcn;
    }

    public synchronized void addOccurrence(ExceptionOccurrence occurrence) {
        occurrences.add(occurrence);
        count = occurrences.size();
        if (occurrence.getTimestamp() != null) {
            if (firstSeen == null || occurrence.getTimestamp().isBefore(firstSeen)) {
                firstSeen = occurrence.getTimestamp();
            }
            if (lastSeen == null || occurrence.getTimestamp().isAfter(lastSeen)) {
                lastSeen = occurrence.getTimestamp();
            }
        }
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
        this.simpleExceptionName = extractSimpleName(exceptionClass);
    }

    public String getSimpleExceptionName() {
        return simpleExceptionName;
    }

    public String getRepresentativeMessage() {
        return representativeMessage;
    }

    public void setRepresentativeMessage(String representativeMessage) {
        this.representativeMessage = representativeMessage;
    }

    public String getTopFrame() {
        return topFrame;
    }

    public void setTopFrame(String topFrame) {
        this.topFrame = topFrame;
    }

    public String getApplicationFrame() {
        return applicationFrame;
    }

    public void setApplicationFrame(String applicationFrame) {
        this.applicationFrame = applicationFrame;
    }

    public String getFullStackTraceSample() {
        return fullStackTraceSample;
    }

    public void setFullStackTraceSample(String fullStackTraceSample) {
        this.fullStackTraceSample = fullStackTraceSample;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public List<ExceptionOccurrence> getOccurrences() {
        return occurrences;
    }
}
