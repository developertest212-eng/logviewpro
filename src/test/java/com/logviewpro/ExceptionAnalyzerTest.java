package com.logviewpro;

import com.logviewpro.index.FileChunkReader;
import com.logviewpro.index.LargeFileIndex;
import com.logviewpro.model.ExceptionGroup;
import com.logviewpro.model.LogEntry;
import com.logviewpro.service.ExceptionAnalyzerService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExceptionAnalyzerTest {

    @Test
    public void testExceptionGroupingAndDiagnosis() {
        File sampleFile = new File("sample-logs/app-production.log");
        LargeFileIndex index = new LargeFileIndex(sampleFile);
        index.buildIndex(null);

        FileChunkReader reader = new FileChunkReader();
        List<LogEntry> entries = reader.readEntriesRange(index, 0, index.size());

        ExceptionAnalyzerService analyzer = new ExceptionAnalyzerService();
        List<ExceptionGroup> groups = analyzer.analyzeExceptions(entries);

        assertFalse(groups.isEmpty(), "Should have identified exception groups");

        // Check that NullPointerException is detected
        boolean foundNpe = groups.stream().anyMatch(g -> "NullPointerException".equals(g.getSimpleExceptionName()));
        assertTrue(foundNpe, "Should detect NullPointerException group");

        // Check that first application frame was extracted correctly (com.logviewpro.demo.UserService)
        ExceptionGroup npeGroup = groups.stream().filter(g -> "NullPointerException".equals(g.getSimpleExceptionName())).findFirst().orElse(null);
        assertNotNull(npeGroup);
        assertNotNull(npeGroup.getApplicationFrame());
        assertTrue(npeGroup.getApplicationFrame().contains("UserService.java"));

        // Check factual explanation generation
        String explanation = analyzer.generateFactualExplanation(npeGroup);
        assertNotNull(explanation);
        assertTrue(explanation.contains("NullPointerException"));
        assertTrue(explanation.contains("UserService"));
    }
}
