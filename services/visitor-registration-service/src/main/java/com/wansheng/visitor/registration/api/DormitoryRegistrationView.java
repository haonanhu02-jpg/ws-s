package com.wansheng.visitor.registration.api;

public record DormitoryRegistrationView(
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
