package com.wansheng.visitor.registration.api;

public record GuardRegistrationView(
        String visitId,
        String visitorName,
        String mobile,
        String hostName,
        String plateNumber,
        boolean vehicleEnteringFactory,
        String oaStatus) {
}

