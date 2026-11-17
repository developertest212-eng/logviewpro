package com.logviewpro.service;

import com.logviewpro.model.ExceptionGroup;
import com.logviewpro.model.ExceptionOccurrence;
import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.util.StacktraceUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExceptionAnalyzerService {

    public List<ExceptionGroup> analyzeExceptions(Collection<LogEntry> entries) {
        Map<String, ExceptionGroup> groupsByFingerprint = new LinkedHashMap<>();

        for (LogEntry entry : entries) {
            if (entry == null) continue;
            boolean isErrorLevel = entry.getLevel() == LogLevel.ERROR || entry.getLevel() == LogLevel.FATAL;
            boolean hasStack = entry.hasStackTrace();

            if (!hasStack && !isErrorLevel) {
                continue;
            }

            String textToAnalyze = hasStack ? entry.getStackTrace() : entry.getMessage();
            String exceptionClass = StacktraceUtil.extractExceptionClass(textToAnalyze);

            if (exceptionClass != null || hasStack) {
                final String finalExClass = (exceptionClass != null) ? exceptionClass : "ApplicationError";
                String extractedMsg = StacktraceUtil.extractExceptionMessage(textToAnalyze);
                if (extractedMsg.isEmpty()) {
                    extractedMsg = entry.getMessage();
                }
                final String finalMsg = extractedMsg;

                String topFrame = StacktraceUtil.extractTopFrame(textToAnalyze);
                String appFrame = StacktraceUtil.extractApplicationFrame(textToAnalyze);
                String fingerprint = StacktraceUtil.generateFingerprint(finalExClass, appFrame != null ? appFrame : topFrame);

                ExceptionGroup group = groupsByFingerprint.computeIfAbsent(fingerprint, k -> {
                    ExceptionGroup g = new ExceptionGroup(fingerprint, finalExClass, finalMsg);
                    g.setTopFrame(topFrame);
                    g.setApplicationFrame(appFrame);
                    g.setFullStackTraceSample(entry.hasStackTrace() ? entry.getStackTrace() : entry.getRawText());
                    return g;
                });

                ExceptionOccurrence occurrence = new ExceptionOccurrence(
                        entry.getLineNumber(),
                        entry.getTimestamp(),
                        entry.getThread(),
                        entry.getMessage(),
                        entry.getStackTrace(),
                        entry.getFilePath()
                );
                group.addOccurrence(occurrence);
            }
        }

        List<ExceptionGroup> result = new ArrayList<>(groupsByFingerprint.values());
        result.sort(Comparator.comparingInt(ExceptionGroup::getCount).reversed());
        return result;
    }

    /**
     * Generates a factual explanation based strictly on the detected exception type and frame location.
     */
    public String generateFactualExplanation(ExceptionGroup group) {
        if (group == null) return "No exception selected.";
        StringBuilder sb = new StringBuilder();

        String exClass = group.getExceptionClass();
        String simpleName = group.getSimpleExceptionName();
        String appFrame = group.getApplicationFrame();
        String msg = group.getRepresentativeMessage();

        sb.append("### Exception Diagnosis: ").append(simpleName).append("\n\n");
        sb.append("**Full Qualified Name:** `").append(exClass).append("`\n\n");

        if (msg != null && !msg.isBlank()) {
            sb.append("**Recorded Error Message:**\n> ").append(msg).append("\n\n");
        }

        if (appFrame != null) {
            sb.append("**First Application Frame:** `").append(appFrame).append("`\n\n");
        }

        sb.append("**Technical Assessment:**\n");
        switch (simpleName) {
            case "NullPointerException" -> sb.append(
                    "- Occurs when an operation is attempted on an uninitialized object reference.\n" +
                    "- Inspect the variables accessed in the application frame above for null checks or unexpected empty responses."
            );
            case "SQLException" -> sb.append(
                    "- Database operation failed during query execution, connection checkout, or transaction commit.\n" +
                    "- Check database connectivity, timeout settings, SQL syntax, or foreign key constraints reported in the message."
            );
            case "IOException", "FileNotFoundException", "SocketException" -> sb.append(
                    "- An I/O or network boundary failure occurred while reading/writing streams or connecting to a remote host.\n" +
                    "- Verify file permissions, path existence, or remote network endpoint availability."
            );
            case "OutOfMemoryError" -> sb.append(
                    "- The JVM has exhausted its allocated heap or native metaspace memory.\n" +
                    "- Review memory allocation flags (-Xmx, -XX:MaxMetaspaceSize) and check for memory retention or unbound caching."
            );
            case "ClassNotFoundException", "NoClassDefFoundError" -> sb.append(
                    "- A required class could not be located on the runtime classpath.\n" +
                    "- Check dependency versions, shaded JAR packaging, or classloader isolation."
            );
            case "BeanCreationException" -> sb.append(
                    "- Spring dependency injection failed while instantiating an application context bean.\n" +
                    "- Check the nested Caused-by section for missing @Autowired dependencies, misconfigured properties, or circular references."
            );
            case "MethodArgumentNotValidException" -> sb.append(
                    "- Request validation failed against controller Bean Validation annotations (e.g. @NotNull, @Size).\n" +
                    "- Inspect request payload structure against data contract specifications."
            );
            default -> sb.append(
                    "- Exception thrown during runtime execution. Refer to the application stack frame line number to trace the exact method call sequence."
            );
        }

        sb.append("\n\n**Total Recorded Occurrences:** ").append(group.getCount());
        if (group.getFirstSeen() != null) {
            sb.append("\n**First Detected:** ").append(group.getFirstSeen());
        }
        if (group.getLastSeen() != null) {
            sb.append("\n**Most Recent:** ").append(group.getLastSeen());
        }

        return sb.toString();
    }

    /**
     * Compares two stack traces line-by-line and highlights shared vs diverging frames.
     */
    public String compareStackTraces(String traceA, String traceB) {
        if (traceA == null) traceA = "";
        if (traceB == null) traceB = "";

        String[] linesA = traceA.split("\r?\n");
        String[] linesB = traceB.split("\r?\n");

        StringBuilder diff = new StringBuilder();
        diff.append("=== Stack Trace Comparison ===\n\n");

        int max = Math.max(linesA.length, linesB.length);
        for (int i = 0; i < max; i++) {
            String a = i < linesA.length ? linesA[i].trim() : "<EOF>";
            String b = i < linesB.length ? linesB[i].trim() : "<EOF>";

            if (a.equals(b)) {
                diff.append("  [SAME]    ").append(a).append("\n");
            } else {
                diff.append("- [TRACE A] ").append(a).append("\n");
                diff.append("+ [TRACE B] ").append(b).append("\n");
            }
        }
        return diff.toString();
    }
}
