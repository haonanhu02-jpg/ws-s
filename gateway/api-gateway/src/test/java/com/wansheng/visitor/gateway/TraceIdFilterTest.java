package com.wansheng.visitor.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TraceIdFilterTest {
    @Test
    void usesStableHeaderName() {
        assertThat(TraceIdFilter.HEADER).isEqualTo("X-Trace-Id");
    }
}

