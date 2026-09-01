package com.wansheng.visitor.common.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class TraceIdTest {
    @Test
    void createsCompactUuid() {
        assertEquals(32, TraceId.create().length());
    }
}

