package com.lanre.personl.iso20022.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoggingContextTest {

    @Test
    void shouldApplyAndRestoreIdentifiers() {
        MDC.clear();
        MDC.put(LoggingContext.MSG_ID, "ORIGINAL-MSG");

        try (LoggingContext.Scope ignored = LoggingContext.withIdentifiers("MSG-123", "E2E-456")) {
            assertEquals("MSG-123", MDC.get(LoggingContext.MSG_ID));
            assertEquals("E2E-456", MDC.get(LoggingContext.END_TO_END_ID));
        }

        assertEquals("ORIGINAL-MSG", MDC.get(LoggingContext.MSG_ID));
        assertNull(MDC.get(LoggingContext.END_TO_END_ID));
        MDC.clear();
    }
}
