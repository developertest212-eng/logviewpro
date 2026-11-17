package com.logviewpro.util;

import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StacktraceUtil {

    private static final Pattern EXCEPTION_HEADER_PATTERN = Pattern.compile(
            "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*([a-zA-Z_$][a-zA-Z\\d_$]*(Exception|Error)):?\\s*(.*)"
    );

    private static final Pattern AT_FRAME_PATTERN = Pattern.compile(
            "^\\s*at\\s+([a-zA-Z_$][a-zA-Z\\d_$.]*)\\.([a-zA-Z_$][a-zA-Z\\d_$]*)\\(([^:]+):?(\\d+)?\\)"
    );

    private static final Pattern CAUSED_BY_PATTERN = Pattern.compile(
            "^Caused by:\\s+([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*([a-zA-Z_$][a-zA-Z\\d_$]*(Exception|Error)):?\\s*(.*)"
    );

    // Framework prefixes to skip when looking for first application-level frame
    private static final String[] FRAMEWORK_PREFIXES = {
            "java.", "javax.", "jakarta.", "jdk.", "sun.", "com.sun.",
            "org.springframework.", "org.apache.catalina.", "org.apache.tomcat.",
            "org.apache.coyote.", "org.hibernate.", "org.jboss.", "io.undertow.",
            "org.eclipse.jetty.", "ch.qos.logback.", "org.apache.logging."
    };

    public static String extractExceptionClass(String text) {
        if (text == null) return null;
        String[] lines = text.split("\r?\n");
        // Look for Caused by first, as root cause often is the true cause
        for (int i = lines.length - 1; i >= 0; i--) {
            Matcher m = CAUSED_BY_PATTERN.matcher(lines[i].trim());
            if (m.find()) {
                return (m.group(1) != null ? m.group(1) : "") + m.group(2);
            }
        }
        // Otherwise look from top
        for (String line : lines) {
            Matcher m = EXCEPTION_HEADER_PATTERN.matcher(line.trim());
            if (m.find()) {
                return (m.group(1) != null ? m.group(1) : "") + m.group(2);
            }
        }
        return null;
    }

    public static String extractExceptionMessage(String text) {
        if (text == null) return "";
        String[] lines = text.split("\r?\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            Matcher m = CAUSED_BY_PATTERN.matcher(lines[i].trim());
            if (m.find()) {
                return m.group(4) != null ? m.group(4).trim() : "";
            }
        }
        for (String line : lines) {
            Matcher m = EXCEPTION_HEADER_PATTERN.matcher(line.trim());
            if (m.find()) {
                return m.group(4) != null ? m.group(4).trim() : "";
            }
        }
        return "";
    }

    public static String extractTopFrame(String text) {
        if (text == null) return null;
        String[] lines = text.split("\r?\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("at ")) {
                return trimmed.substring(3).trim();
            }
        }
        return null;
    }

    public static String extractApplicationFrame(String text) {
        if (text == null) return null;
        String[] lines = text.split("\r?\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("at ")) {
                String frame = trimmed.substring(3).trim();
                boolean isFramework = false;
                for (String prefix : FRAMEWORK_PREFIXES) {
                    if (frame.startsWith(prefix)) {
                        isFramework = true;
                        break;
                    }
                }
                if (!isFramework) {
                    return frame;
                }
            }
        }
        return extractTopFrame(text);
    }

    public static String generateFingerprint(String exceptionClass, String topOrAppFrame) {
        String base = (exceptionClass != null ? exceptionClass : "UnknownException") + ":" +
                      (topOrAppFrame != null ? topOrAppFrame.replaceAll(":\\d+\\)", ")") : "UnknownFrame");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(base.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(base.hashCode());
        }
    }
}
