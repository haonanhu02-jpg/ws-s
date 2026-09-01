package com.wansheng.visitor.registration.api;

import java.time.Instant;

public record RegistrationResultResponse(
        String visitId,
        String registrationStatus,
        String oaStatus,
        Instant registeredAt,
        String notice) {
}

