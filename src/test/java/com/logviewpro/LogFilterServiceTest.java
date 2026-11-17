package com.logviewpro;

import com.logviewpro.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class LogFilterServiceTest {

    @Test
    public void testFilterConditions() {
        LogEntry entry = new LogEntry(1, 1, LocalDateTime.now(), "2026-11-17 01:00:00",
                LogLevel.ERROR, "http-worker-1", "com.example.UserService", "User login failed with NPE",
                "java.lang.NullPointerException\n\tat com.example.UserService.run(UserService.java:10)", "", "");

        // 1. Level condition
        FilterCondition levelCond = new FilterCondition(FilterField.LEVEL, FilterOperator.EQUALS, "ERROR");
        assertTrue(levelCond.test(entry));

        // 2. Message contains
        FilterCondition msgCond = new FilterCondition(FilterField.MESSAGE, FilterOperator.CONTAINS, "login failed");
        assertTrue(msgCond.test(entry));

        // 3. Logger contains
        FilterCondition loggerCond = new FilterCondition(FilterField.LOGGER, FilterOperator.CONTAINS, "UserService");
        assertTrue(loggerCond.test(entry));

        // 4. Combined Filter AND
        LogFilter andFilter = new LogFilter("Errors in UserService");
        andFilter.setLogic(LogFilter.Logic.AND);
        andFilter.addCondition(levelCond);
        andFilter.addCondition(loggerCond);
        assertTrue(andFilter.test(entry));

        // 5. Exclude Mode
        andFilter.setExclude(true);
        assertFalse(andFilter.test(entry));
    }
}
