package com.wansheng.visitor.registration.service;

import com.wansheng.visitor.registration.api.CreateRegistrationRequest;
import com.wansheng.visitor.registration.domain.PhoneNotificationMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RegistrationValidator {
    public void validate(CreateRegistrationRequest request, PhoneNotificationMode phoneMode) {
        if (Boolean.TRUE.equals(request.hasVehicle()) && !StringUtils.hasText(request.plateNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "有车辆时必须填写车牌号");
        }
        if (!Boolean.TRUE.equals(request.hasVehicle()) && StringUtils.hasText(request.plateNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无车辆时不得填写车牌号");
        }
        if (!Boolean.TRUE.equals(request.hasVehicle()) && Boolean.TRUE.equals(request.vehicleEnteringFactory())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无车辆时不能选择开车进厂");
        }
        if (Boolean.TRUE.equals(request.phoneNotificationRequested()) && phoneMode == PhoneNotificationMode.UNDECIDED) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Q-001～Q-003 尚未确认，电话通知暂不可选");
        }
    }
}
