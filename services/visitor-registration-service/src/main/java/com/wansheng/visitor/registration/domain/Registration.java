package com.wansheng.visitor.registration.domain;

import java.time.Instant;

public record Registration(
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
        RegistrationStatus registrationStatus,
        OaStatus oaStatus,
        Instant registeredAt) {
}
