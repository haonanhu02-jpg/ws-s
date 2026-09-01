package com.wansheng.visitor.registration.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wansheng.visitor.registration.api.CreateRegistrationRequest;
import com.wansheng.visitor.registration.domain.PhoneNotificationMode;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RegistrationValidatorTest {
    private final RegistrationValidator validator = new RegistrationValidator();

    @Test
    void acceptsStandardRegistrationWithoutIdentityCard() {
        assertDoesNotThrow(() -> validator.validate(validRequest(), PhoneNotificationMode.UNDECIDED));
    }

    @Test
    void requiresPlateNumberWhenVisitorHasVehicle() {
        CreateRegistrationRequest request = new CreateRegistrationRequest(
                "张三", "13800138000", "李经理", "生产部", "项目交流",
                false, true, null, true, false);
        assertThrows(ResponseStatusException.class,
                () -> validator.validate(request, PhoneNotificationMode.UNDECIDED));
    }
    @Test void blocksRequestedPhoneNotificationWhileModeIsUndecided(){CreateRegistrationRequest r=new CreateRegistrationRequest("张三","13800138000","李经理","生产部","交流",false,false,null,false,true);assertThrows(ResponseStatusException.class,()->validator.validate(r,PhoneNotificationMode.UNDECIDED));}

    private static CreateRegistrationRequest validRequest() {
        return new CreateRegistrationRequest(
                "张三", "13800138000", "李经理", "生产部", "项目交流",
                false, false, null, false, false);
    }
}
