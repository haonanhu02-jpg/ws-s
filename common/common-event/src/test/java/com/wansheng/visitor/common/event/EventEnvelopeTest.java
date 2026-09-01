package com.wansheng.visitor.common.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {
    @Test
    void rejectsMissingVisitId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventEnvelope("event", "VISITOR_REGISTERED", "", Instant.now(), 1));
    }
}

