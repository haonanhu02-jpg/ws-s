package com.wansheng.visitor.common.event;

import java.time.Instant;

public record EventEnvelope(
        String eventId,
        String eventType,
        String visitId,
        Instant occurredAt,
        int version) {
    public EventEnvelope {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("eventId is required");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType is required");
        if (visitId == null || visitId.isBlank()) throw new IllegalArgumentException("visitId is required");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt is required");
        if (version < 1) throw new IllegalArgumentException("version must be positive");
    }
}

