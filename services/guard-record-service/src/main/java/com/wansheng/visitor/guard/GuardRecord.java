package com.wansheng.visitor.guard;
import java.time.Instant;
public record GuardRecord(String visitId, String visitorName, String mobile, String hostName,
        String plateNumber, boolean vehicleEnteringFactory, boolean accommodationRequired,
        String oaStatus, GuardStatus guardStatus,
        Instant entryTime, Instant exitTime, String entryOperator, String exitOperator) {}
