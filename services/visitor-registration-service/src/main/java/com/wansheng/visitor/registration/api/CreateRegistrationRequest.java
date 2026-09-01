package com.wansheng.visitor.registration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRegistrationRequest(
        @NotBlank @Size(max = 100) String visitorName,
        @NotBlank @Pattern(regexp = "^[0-9+ -]{6,30}$") String mobile,
        @NotBlank @Size(max = 100) String hostName,
        @NotBlank @Size(max = 150) String hostDepartment,
        @NotBlank @Size(max = 500) String visitReason,
        @NotNull Boolean accommodationRequired,
        @NotNull Boolean hasVehicle,
        @Size(max = 30) String plateNumber,
        @NotNull Boolean vehicleEnteringFactory,
        Boolean phoneNotificationRequested) {
}
