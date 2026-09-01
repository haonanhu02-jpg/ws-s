package com.wansheng.visitor.registration.api;

import com.wansheng.visitor.registration.config.RegistrationProperties;
import com.wansheng.visitor.registration.domain.Registration;
import com.wansheng.visitor.registration.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.wansheng.visitor.registration.domain.OaStatus;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/registrations")
public class InternalRegistrationController {
    private static final String TOKEN_HEADER = "X-Internal-Token";
    private final RegistrationService service;
    private final RegistrationProperties properties;

    public InternalRegistrationController(RegistrationService service, RegistrationProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/{visitId}/guard-view")
    GuardRegistrationView guardView(
            @PathVariable String visitId,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        verifyToken(token);
        Registration item = find(visitId);
        return new GuardRegistrationView(
                item.visitId(), item.visitorName(), item.mobile(), item.hostName(),
                item.plateNumber(), item.vehicleEnteringFactory(), item.oaStatus().name());
    }

    @GetMapping("/{visitId}/dormitory-view")
    DormitoryRegistrationView dormitoryView(
            @PathVariable String visitId,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        verifyToken(token);
        Registration item = find(visitId);
        return new DormitoryRegistrationView(
                item.visitId(), item.visitorName(), item.mobile(), item.hostName(),
                item.hostDepartment(), item.visitReason(),
                item.accommodationRequired(), item.hasVehicle(), item.plateNumber(),
                item.vehicleEnteringFactory());
    }

    @GetMapping("/{visitId}/oa-view")
    OaRegistrationView oaView(
            @PathVariable String visitId,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        verifyToken(token);
        Registration item = find(visitId);
        return new OaRegistrationView(
                item.visitId(), item.visitorName(), item.mobile(), item.hostName(),
                item.hostDepartment(), item.visitReason(),
                item.accommodationRequired(), item.hasVehicle(), item.plateNumber(),
                item.vehicleEnteringFactory());
    }

    @GetMapping("/admin-view")
    List<AdminRegistrationView> adminView(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        verifyToken(token);
        return service.findRecent(limit).stream().map(item -> new AdminRegistrationView(
                item.visitId(), item.visitorName(), item.mobile(), item.hostName(),
                item.hostDepartment(), item.visitReason(), item.accommodationRequired(),
                item.hasVehicle(), item.plateNumber(), item.vehicleEnteringFactory(),
                item.phoneNotificationRequested(), item.registrationStatus().name(), item.registeredAt()))
                .toList();
    }

    @PutMapping("/{visitId}/oa-status")
    void updateOaStatus(@PathVariable String visitId, @RequestBody OaStatusRequest request,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        verifyToken(token);
        service.updateOaStatus(visitId, OaStatus.valueOf(request.status()));
    }
    record OaStatusRequest(String status) {}

    private Registration find(String visitId) {
        return service.find(visitId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "来访记录不存在"));
    }

    private void verifyToken(String token) {
        String configured = properties.internalToken();
        if (configured == null || configured.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "内部接口令牌未配置");
        }
        if (!configured.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问内部视图");
        }
    }
}
