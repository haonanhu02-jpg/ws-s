package com.wansheng.visitor.guard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record GuardRecordRequest(
        @NotBlank @Size(max = 100) String visitorName,
        @NotBlank @Pattern(regexp = "^[0-9+ -]{6,30}$") String mobile,
        @NotBlank @Size(max = 100) String hostName,
        @Size(max = 30) String plateNumber,
        @NotNull Boolean vehicleEnteringFactory) {
}
