package com.wansheng.visitor.registration.api;

public record OaRegistrationView(
        String visitId,
        String visitorName,
        String mobile,
        String hostName,
        String hostDepartment,
        String visitReason,
        boolean accommodationRequired,
        boolean hasVehicle,
        String plateNumber,
        boolean vehicleEnteringFactory) {
}
