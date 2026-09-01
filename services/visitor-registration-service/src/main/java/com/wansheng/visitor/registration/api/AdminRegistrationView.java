package com.wansheng.visitor.registration.api;

import java.time.Instant;

public record AdminRegistrationView(
        String visitId,
        String visitorName,
        String mobile,
        String hostName,
        String hostDepartment,
        String visitReason,
        boolean accommodationRequired,
        boolean hasVehicle,
        String plateNumber,
        boolean vehicleEnteringFactory,
        boolean phoneNotificationRequested,
        String registrationStatus,
        Instant registeredAt) {
}

