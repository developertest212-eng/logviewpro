package com.logviewpro;

import com.logviewpro.model.LogEntry;
import com.logviewpro.model.LogLevel;
import com.logviewpro.parser.CompositeLogParser;
import com.logviewpro.parser.JsonLogParser;
import com.logviewpro.parser.SpringBootLogParser;
import com.logviewpro.parser.StandardLogParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LogParserTest {

    @Test
    public void testStandardLogParser() {
        StandardLogParser parser = new StandardLogParser();
        String line = "2026-11-17 01:00:05.100 [http-nio-8080-exec-1] INFO com.logviewpro.demo.OrderController - Received POST /orders";

        assertTrue(parser.isNewEntry(line));
        LogEntry entry = parser.parseHeader(line, 1, "test.log");

        assertNotNull(entry);
        assertEquals(LogLevel.INFO, entry.getLevel());
        assertEquals("http-nio-8080-exec-1", entry.getThread());
        assertEquals("com.logviewpro.demo.OrderController", entry.getLogger());
        assertEquals("Received POST /orders", entry.getMessage());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    public void testSpringBootLogParser() {
        SpringBootLogParser parser = new SpringBootLogParser();
        String line = "2026-11-17T01:10:00.120+00:00  INFO 28412 --- [           main] c.l.demo.SpringBootDemoApplication       : Starting application";

        assertTrue(parser.isNewEntry(line));
        LogEntry entry = parser.parseHeader(line, 1, "test.log");

        assertNotNull(entry);
        assertEquals(LogLevel.INFO, entry.getLevel());
        assertEquals("main", entry.getThread());
        assertEquals("c.l.demo.SpringBootDemoApplication", entry.getLogger());
        assertEquals("Starting application", entry.getMessage());
    }

    @Test
    public void testJsonLogParser() {
        JsonLogParser parser = new JsonLogParser();
        String jsonLine = "{\"timestamp\":\"2026-11-17T01:20:00.100Z\",\"level\":\"ERROR\",\"thread\":\"worker-1\",\"logger\":\"PaymentService\",\"message\":\"Payment failed\",\"stack_trace\":\"java.lang.NullPointerException\"}";

        assertTrue(parser.isNewEntry(jsonLine));
        LogEntry entry = parser.parseHeader(jsonLine, 1, "test.jsonl");

        assertNotNull(entry);
        assertEquals(LogLevel.ERROR, entry.getLevel());
        assertEquals("worker-1", entry.getThread());
        assertEquals("PaymentService", entry.getLogger());
        assertEquals("Payment failed", entry.getMessage());
        assertTrue(entry.hasStackTrace());
    }

    @Test
    public void testCompositeLogParserWithContinuation() {
        CompositeLogParser parser = new CompositeLogParser();
        String header = "2026-11-17 01:01:16.020 [main] ERROR com.example.Service - Null pointer";
        String stack1 = "\tat com.example.Service.run(Service.java:45)";
        String stack2 = "\tat java.base/java.lang.Thread.run(Thread.java:840)";

        assertTrue(parser.isNewEntry(header));
        assertFalse(parser.isNewEntry(stack1));
        assertFalse(parser.isNewEntry(stack2));

        LogEntry entry = parser.parseHeader(header, 1, "test.log");
        parser.appendContinuation(entry, stack1);
        parser.appendContinuation(entry, stack2);

        assertTrue(entry.hasStackTrace());
        assertTrue(entry.getStackTrace().contains("Service.java:45"));
    }
}
