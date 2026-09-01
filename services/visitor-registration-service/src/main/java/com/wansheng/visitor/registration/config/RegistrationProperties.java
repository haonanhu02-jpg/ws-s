package com.wansheng.visitor.registration.config;

import com.wansheng.visitor.registration.domain.PhoneNotificationMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("visitor.registration")
public record RegistrationProperties(
        String internalToken,
        PhoneNotificationMode phoneNotificationMode) {
    public RegistrationProperties {
        phoneNotificationMode = phoneNotificationMode == null ? PhoneNotificationMode.UNDECIDED : phoneNotificationMode;
    }
}
